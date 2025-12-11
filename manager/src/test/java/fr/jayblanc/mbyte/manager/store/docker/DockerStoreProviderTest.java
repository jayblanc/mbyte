package fr.jayblanc.mbyte.manager.store.docker;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DockerStoreProviderTest {

    @Test
    void createAppRunsAgainstRealDockerClient() throws Exception {
        DockerStoreProvider provider = new DockerStoreProvider();
        injectConfig(provider, new DockerStoreProviderConfig() {
            @Override
            public String server() {
                return System.getProperty("test.docker.host", "unix:///var/run/docker.sock");
            }

            @Override
            public String image() {
                return "jerome/store:25.1-SNAPSHOT";
            }

            @Override
            public Workdir workdir() {
                return new Workdir() {
                    @Override
                    public String host() {
                        return "/tmp/stores";
                    }

                    @Override
                    public String local() {
                        return "/home/jboss/mbyte/data/stores";
                    }
                };
            }
        });
        invokeInit(provider);

        assertDoesNotThrow(() -> provider.createStore("42", "owner", "My store"));
    }

    private static void injectConfig(DockerStoreProvider provider, DockerStoreProviderConfig config) throws Exception {
        Field field = DockerStoreProvider.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(provider, config);
    }

    private static void invokeInit(DockerStoreProvider provider) throws Exception {
        Method init = DockerStoreProvider.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(provider);
    }
}
