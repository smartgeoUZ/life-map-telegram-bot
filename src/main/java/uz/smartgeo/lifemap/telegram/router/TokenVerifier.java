package uz.smartgeo.lifemap.telegram.router;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenVerifier implements AuthProvider {

    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        String token = credentials.getString("jwt");
        try {
            String tokenInfo = verifyToken(token);

//            List<String> permissions = findPermissions(tokenInfo);
//            User user = new User(name, permissions);
//            resultHandler.handle(Future.succeededFuture(user));
        } catch (Exception e) {
            resultHandler.handle(Future.failedFuture(e));
        }
    }

    private String verifyToken(String token)  {

        return "asd";
    }

}