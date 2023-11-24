package es.cursonoruego.util;

import es.cursonoruego.model.enums.Environment;

public class EnvironmentSettings {

    public static final String DOMAIN = "cursonoruego.es";

//      public static final Environment ENVIRONMENT = Environment.DEV;
//       public static final Environment ENVIRONMENT = Environment.TEST;
    public static final Environment ENVIRONMENT = Environment.PROD;

    public static String getBaseUrl() {
        if (ENVIRONMENT == Environment.DEV) {
            return "http://192.168.1.129:8080/course-webapp";
        } else if (ENVIRONMENT == Environment.TEST) {
            return "https://test." + DOMAIN;
        } else {
            return "https://" + DOMAIN;
        }
    }
}
