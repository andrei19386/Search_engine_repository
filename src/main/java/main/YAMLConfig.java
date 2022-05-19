package main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Этот класс нужен для чтения параметров из файла application.yml (Параметров, записываемых в этот файл
 * в соответствии с техническим заданием)
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
class YAMLConfig {

    private List<SiteRead> sites = new ArrayList<>();

    private String userAgent;
    private String pathToInterface;

    public List<SiteRead> getSites() {
        return sites;
    }

    public void setSites(List<SiteRead> sites) {
        this.sites = sites;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPathToInterface() {
        return pathToInterface;
    }

    public void setPathToInterface(String pathToInterface) {
        this.pathToInterface = pathToInterface;
    }

    public static class SiteRead {
        private String url;
        private String name;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
