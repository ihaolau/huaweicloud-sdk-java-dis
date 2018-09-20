package com.huaweicloud.dis.util;

import com.huaweicloud.dis.DISConfig;
import com.huaweicloud.dis.core.http.HttpMethodName;
import com.huaweicloud.dis.exception.DISClientException;
import com.huaweicloud.dis.iface.api.protobuf.Message;
import com.huaweicloud.dis.iface.app.response.CreateAppResult;
import com.huaweicloud.dis.iface.app.response.DeleteAppResult;
import com.huaweicloud.dis.iface.app.response.DescribeAppResult;
import com.huaweicloud.dis.iface.app.response.ListAppsResult;
import com.huaweicloud.dis.iface.data.response.*;
import com.huaweicloud.dis.iface.stream.response.*;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RestClient
{
    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private ICallMaker callMaker = null;

    private static RestClient restClient;

    public OkHttpClient createClient(DISConfig disConfig)
    {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(disConfig.getMaxTotal());
        dispatcher.setMaxRequestsPerHost(disConfig.getMaxPerRoute());

        OkHttpClient.Builder builder = new OkHttpClient.Builder().followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true)
                .connectTimeout(disConfig.getConnectionTimeOut(), TimeUnit.MILLISECONDS)
                .writeTimeout(disConfig.getSocketTimeOut(), TimeUnit.MILLISECONDS)
                .readTimeout(disConfig.getSocketTimeOut(), TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .hostnameVerifier(new TrustAllHostnameVerifier());

        if (LOG.isDebugEnabled())
        {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        // 禁用客户端证书校验
        if (!disConfig.getIsDefaultTrustedJksEnabled())
        {
            builder.sslSocketFactory(getSSLSocketFactory(), (X509TrustManager) getTrustManager()[0]);
        }

        return builder.build();
    }

    private RestClient(DISConfig disConfig)
    {
        OkHttpClient instance = createClient(disConfig);
        Retrofit retrofit = new Retrofit.Builder().baseUrl(disConfig.getEndpoint())
                .addConverterFactory(ProtoConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .client(instance)
                .build();

        callMaker = retrofit.create(ICallMaker.class);
    }


    public synchronized static RestClient getInstance(DISConfig disConfig)
    {
        if (restClient == null)
        {
            restClient = new RestClient(disConfig);
        }

        return restClient;
    }

    private static SSLSocketFactory getSSLSocketFactory()
    {
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * 忽略对服务器端证书的校验
     *
     * @return
     */
    private static TrustManager[] getTrustManager()
    {
        return new TrustManager[]{new X509TrustManager()
        {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
            {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
            {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return new X509Certificate[]{};
            }
        }};
    }

    /**
     * 信任所有主机名
     */
    private static class TrustAllHostnameVerifier implements HostnameVerifier
    {
        @Override
        public boolean verify(String hostname, SSLSession session)
        {
            return true;
        }
    }

    public <T> T request(Call<T> call)
    {
        Response response;
        try
        {
            response = call.execute();
            if (response.isSuccessful())
            {
                return response.body() == null ? null : (T) response.body();
            }
            else
            {
                throw new DISClientException(response.code() + " " + response.message()
                        + (response.errorBody() == null ? "" : " : " + response.errorBody().string()));
            }
        }
        catch (IOException e)
        {
            throw new DISClientException(e);
        }
    }

    public <T> T exchange(String url, HttpMethodName httpMethod, Map<String, String> headers, Object requestContent,
                          Class<T> responseClazz)
    {
        if (PutRecordsResult.class.equals(responseClazz))
        {
            // json发送数据
            return (T) request(callMaker.putRecords(url, headers, getRequestBody(requestContent)));
        }
        else if (GetRecordsResult.class.equals(responseClazz))
        {
            // json获取数据
            return (T) request(callMaker.getRecords(url, headers));
        }
        else if (Message.PutRecordsResult.class.equals(responseClazz))
        {
            // protobuf发送数据
            return (T) request(callMaker.putRecordsProto(url, headers, getRequestBody(requestContent)));
        }
        else if (Message.GetRecordsResult.class.equals(responseClazz))
        {
            // protobuf下载数据
            return (T) request(callMaker.getRecordsProto(url, headers));
        }
        else if (GetPartitionCursorResult.class.equals(responseClazz))
        {
            // 获取迭代器
            return (T) request(callMaker.getPartitionCursor(url, headers));
        }
        else if (CommitCheckpointResult.class.equals(responseClazz))
        {
            // 提交checkpoint
            return (T) request(callMaker.commitCheckpoint(url, headers, getRequestBody(requestContent)));
        }
        else if (GetCheckpointResult.class.equals(responseClazz))
        {
            // 获取checkpoint
            return (T) request(callMaker.getCheckpoint(url, headers));
        }
        else if (DescribeStreamResult.class.equals(responseClazz))
        {
            // 查询通道详情
            return (T) request(callMaker.describeStream(url, headers));
        }
        else if (CreateStreamResult.class.equals(responseClazz))
        {
            // 创建通道
            return (T) request(callMaker.createStream(url, headers, getRequestBody(requestContent)));
        }
        else if (DeleteStreamResult.class.equals(responseClazz))
        {
            // 删除通道
            return (T) request(callMaker.deleteStream(url, headers));
        }
        else if (ListStreamsResult.class.equals(responseClazz))
        {
            // 通道列表
            return (T) request(callMaker.listStreams(url, headers));
        }
        else if (CreateAppResult.class.equals(responseClazz))
        {
            // 创建APP
            return (T) request(callMaker.createApp(url, headers, getRequestBody(requestContent)));
        }
        else if (ListAppsResult.class.equals(responseClazz))
        {
            // 查询APP列表
            return (T) request(callMaker.listApps(url, headers));
        }
        else if (DeleteAppResult.class.equals(responseClazz))
        {
            // 删除APP
            return (T) request(callMaker.deleteApp(url, headers));
        }
        else if (DescribeAppResult.class.equals(responseClazz))
        {
            // 查询APP详情
            return (T) request(callMaker.describeApp(url, headers));
        }
        else if (UpdatePartitionCountResult.class.equals(responseClazz))
        {
            // 更新分区数量
            return (T) request(callMaker.updatePartitionCountResult(url, headers, getRequestBody(requestContent)));
        }
        else if (FileUploadResult.class.equals(responseClazz))
        {
            // 获取文件状态
            return (T) request(callMaker.getFileUploadResult(url, headers));
        }

        throw new DISClientException("unimplemented.");
    }

    public RequestBody getRequestBody(Object requestContent)
    {
        RequestBody body;
        if (requestContent != null)
        {
            if (requestContent instanceof byte[])
            {
                body = RequestBody.create(null, (byte[]) requestContent);
            }
            else if (requestContent instanceof String || requestContent instanceof Integer)
            {
                body = RequestBody.create(null, Utils.encodingBytes(requestContent.toString()));
            }
            else
            {
                String reqJson = JsonUtils.objToJson(requestContent);
                body = RequestBody.create(null, Utils.encodingBytes(reqJson));
            }
        }
        else
        {
            body = RequestBody.create(null, "".getBytes());
        }
        return body;
    }

}
