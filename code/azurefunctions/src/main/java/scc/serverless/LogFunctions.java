package scc.serverless;

import java.util.*;

import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;

import com.microsoft.azure.functions.*;

import static scc.cache.RedisCache.LOG_KEY;

/**
 * Azure Functions with HTTP Trigger. These functions can be accessed at:
 * {Server_URL}/api/{route}
 * Complete URL appear when deploying functions.
 */
public class LogFunctions {


    @FunctionName("functions-log")
    public HttpResponseMessage getFunctionsLog(@HttpTrigger(name = "req",
            methods = {HttpMethod.GET },
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "serverless/log")
                                            HttpRequestMessage<Optional<String>> request,
                                    final ExecutionContext context) {


        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String val = jedis.get(LOG_KEY);

            return request.createResponseBuilder(HttpStatus.OK).body(val).build();

        } catch (Exception e) {
            // Log the exception
            context.getLogger().severe("Error: " + e.getMessage());

            // Return an error response
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error").build();
        }

    }

    @FunctionName("clear-functions-log")
    public void clearFunctionsLog(@HttpTrigger(name = "req",
            methods = {HttpMethod.POST },
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "serverless/log")
                                                       HttpRequestMessage<Optional<String>> request,
                                               final ExecutionContext context) {


        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            jedis.set(LOG_KEY, "");


        } catch (Exception e) {
            // Log the exception
            context.getLogger().severe("Error: " + e.getMessage());

        }

    }

}
