package book.video;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.assertions.DockerJavaAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class VideoServiceContainerTest {


    @ArquillianResource
    DockerClient docker;

    @Test
    public void should_create_valid_dockerfile() {
        DockerJavaAssertions.assertThat(docker).container
                ("videoservice").hasExposedPorts("8080/tcp")
                .isRunning();
    }

}