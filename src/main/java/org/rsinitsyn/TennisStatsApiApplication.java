package org.rsinitsyn;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

@ApplicationPath("/api")
@OpenAPIDefinition(
        info = @Info(title = "Tennis Stats API", version = "3.0.0")
//        components = @Components(
//                securitySchemes = {@SecurityScheme(
//                        securitySchemeName = "basic",
//                        type = SecuritySchemeType.HTTP,
//                        scheme = "basic"
//                )}),
//        security = @SecurityRequirement(name = "basic")
)
public class TennisStatsApiApplication extends Application {

//    @Transactional
//    public void loadUsers(@Observes StartupEvent evt) {
//        User.deleteAll();
//        User.add("admin", "admin", "admin");
//    }
}
