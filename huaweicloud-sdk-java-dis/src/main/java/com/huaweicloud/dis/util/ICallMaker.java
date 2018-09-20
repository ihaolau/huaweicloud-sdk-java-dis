package com.huaweicloud.dis.util;

import com.huaweicloud.dis.iface.api.protobuf.Message;
import com.huaweicloud.dis.iface.app.response.DeleteAppResult;
import com.huaweicloud.dis.iface.app.response.DescribeAppResult;
import com.huaweicloud.dis.iface.app.response.ListAppsResult;
import com.huaweicloud.dis.iface.data.response.*;
import com.huaweicloud.dis.iface.stream.response.*;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface ICallMaker
{
    /**
     * Json请求体上传
     */
    @POST
    Call<PutRecordsResult> putRecords(@Url String url, @HeaderMap Map<String, String> headers, @Body RequestBody body);

    /**
     * Json请求体下载
     */
    @GET
    Call<GetRecordsResult> getRecords(@Url String url, @HeaderMap Map<String, String> headers);

    /**
     * Protobuf请求体上传
     */
    @POST
    Call<Message.PutRecordsResult> putRecordsProto(@Url String url, @HeaderMap Map<String, String> headers, @Body RequestBody body);

    /**
     * Protobuf请求体下载
     */
    @GET
    Call<Message.GetRecordsResult> getRecordsProto(@Url String url, @HeaderMap Map<String, String> headers);

    @GET
    Call<GetPartitionCursorResult> getPartitionCursor(@Url String url, @HeaderMap Map<String, String> headers);

    @POST
    Call<Void> commitCheckpoint(@Url String url, @HeaderMap Map<String, String> headers, @Body RequestBody body);

    @GET
    Call<GetCheckpointResult> getCheckpoint(@Url String url, @HeaderMap Map<String, String> headers);

    @GET
    Call<DescribeStreamResult> describeStream(@Url String url, @HeaderMap Map<String, String> headers);

    @POST
    Call<Void> createStream(@Url String url, @HeaderMap Map<String, String> headers, @Body RequestBody body);

    @DELETE
    Call<DeleteStreamResult> deleteStream(@Url String url, @HeaderMap Map<String, String> headers);

    @GET
    Call<ListStreamsResult> listStreams(@Url String url, @HeaderMap Map<String, String> headers);

    @POST
    Call<Void> createApp(@Url String url, @HeaderMap Map<String, String> headers, @Body RequestBody body);

    @GET
    Call<ListAppsResult> listApps(@Url String url, @HeaderMap Map<String, String> headers);

    @DELETE
    Call<DeleteAppResult> deleteApp(@Url String url, @HeaderMap Map<String, String> headers);

    @GET
    Call<DescribeAppResult> describeApp(@Url String url, @HeaderMap Map<String, String> headers);

    @PUT
    Call<UpdatePartitionCountResult> updatePartitionCountResult(@Url String url, @HeaderMap Map<String, String> headers, @Body RequestBody body);

    @GET
    Call<FileUploadResult> getFileUploadResult(@Url String url, @HeaderMap Map<String, String> headers);

}
