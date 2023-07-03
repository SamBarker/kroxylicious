/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter.multitenant;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.kafka.common.message.ApiMessageType;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import io.kroxylicious.proxy.filter.FilterAndInvoker;
import io.kroxylicious.proxy.filter.FilterInvoker;
import io.kroxylicious.proxy.filter.KrpcFilterContext;
import io.kroxylicious.test.requestresponsetestdef.ApiMessageTestDef;
import io.kroxylicious.test.requestresponsetestdef.RequestResponseTestDef;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static io.kroxylicious.test.requestresponsetestdef.KafkaApiMessageConverter.requestConverterFor;
import static io.kroxylicious.test.requestresponsetestdef.KafkaApiMessageConverter.responseConverterFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiTenantTransformationFilterTest {
    private static final Pattern TEST_RESOURCE_FILTER = Pattern.compile(
            String.format("%s/.*\\.yaml", MultiTenantTransformationFilterTest.class.getPackageName().replace(".", "/")));

    private static List<ResourceInfo> getTestResources() throws IOException {
        var resources = ClassPath.from(MultiTenantTransformationFilterTest.class.getClassLoader()).getResources().stream()
                .filter(ri -> TEST_RESOURCE_FILTER.matcher(ri.getResourceName()).matches()).toList();

        // https://youtrack.jetbrains.com/issue/IDEA-315462: we've seen issues in IDEA in IntelliJ Workspace Model API mode where test resources
        // don't get added to the Junit runner classpath. You can work around by not using that mode, or by adding src/test/resources to the
        // runner's classpath using 'modify classpath' option in the dialogue.
        checkState(!resources.isEmpty(), "no test resource files found on classpath matching %s", TEST_RESOURCE_FILTER);

        return resources;
    }

    private final MultiTenantTransformationFilter filter = new MultiTenantTransformationFilter();

    private final FilterInvoker invoker = getOnlyElement(FilterAndInvoker.build(filter)).invoker();

    private final KrpcFilterContext context = mock(KrpcFilterContext.class);

    private final ArgumentCaptor<ApiMessage> apiMessageCaptor = ArgumentCaptor.forClass(ApiMessage.class);

    @BeforeEach
    public void beforeEach() {
        when(context.sniHostname()).thenReturn("tenant1.kafka.example.com");
    }

    public static Stream<Arguments> requests() throws Exception {
        return RequestResponseTestDef.requestResponseTestDefinitions(getTestResources()).filter(td -> td.request() != null)
                .map(td -> Arguments.of(td.testName(), td.apiKey(), td.header(), td.request()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "requests")
    void requestsTransformed(@SuppressWarnings("unused") String testName, ApiMessageType apiMessageType, RequestHeaderData header, ApiMessageTestDef requestTestDef) {
        var request = requestTestDef.message();
        // marshalled the request object back to json, this is used for the comparison later.
        var requestWriter = requestConverterFor(apiMessageType).writer();
        var marshalled = requestWriter.apply(request, header.requestApiVersion());

        invoker.onRequest(ApiKeys.forId(apiMessageType.apiKey()), header.requestApiVersion(), header, request, context);
        verify(context).forwardRequest(any(), apiMessageCaptor.capture());

        var filtered = requestWriter.apply(apiMessageCaptor.getValue(), header.requestApiVersion());
        assertEquals(requestTestDef.expectedPatch(), JsonDiff.asJson(marshalled, filtered));
    }

    public static Stream<Arguments> responses() throws Exception {
        return RequestResponseTestDef.requestResponseTestDefinitions(getTestResources()).filter(td -> td.response() != null)
                .map(td -> Arguments.of(td.testName(), td.apiKey(), td.header(), td.response()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "responses")
    void responseTransformed(@SuppressWarnings("unused") String testName, ApiMessageType apiMessageType, RequestHeaderData header, ApiMessageTestDef responseTestDef) {
        var response = responseTestDef.message();
        // marshalled the response object back to json, this is used for comparison later.
        var responseWriter = responseConverterFor(apiMessageType).writer();

        var marshalled = responseWriter.apply(response, header.requestApiVersion());

        ResponseHeaderData headerData = new ResponseHeaderData();
        invoker.onResponse(ApiKeys.forId(apiMessageType.apiKey()), header.requestApiVersion(), headerData, response, context);
        verify(context).forwardResponse(any(), apiMessageCaptor.capture());

        var filtered = responseWriter.apply(apiMessageCaptor.getValue(), header.requestApiVersion());
        assertEquals(responseTestDef.expectedPatch(), JsonDiff.asJson(marshalled, filtered));
    }
}