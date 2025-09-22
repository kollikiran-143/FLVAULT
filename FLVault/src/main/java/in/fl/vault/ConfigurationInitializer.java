package in.fl.vault;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

public class ConfigurationInitializer implements ApplicationContextInitializer {
	private static final String EXTERNAL_PROPS = "file:/FRNDLN/Config/flvault.properties";

	public void initialize(ConfigurableApplicationContext applicationContext) {
		try {
			final Resource[] externalConfigs = applicationContext.getResources(EXTERNAL_PROPS);

			final ConfigurableEnvironment env = applicationContext.getEnvironment();
			final MutablePropertySources mps = env.getPropertySources();
			for (Resource r : externalConfigs) {
				mps.addLast(new ResourcePropertySource(r.getFilename(), r));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}