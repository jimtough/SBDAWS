package com.jimtough.sbdaws;

import static com.jimtough.sbdaws.ConfigurationBean.SYSPROPKEY_AWS_ACCESS_KEY_ID;
import static com.jimtough.sbdaws.ConfigurationBean.SYSPROPKEY_AWS_SECRET_ACCESS_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import software.amazon.awssdk.regions.Region;

/**
 * Use to load the app.properties resource file from the classpath
 * 
 * @author JTOUGH
 */
public class PropertiesLoader {

	private static final String PROPERTIES_RESOURCE_FILE_NAME = "app.properties";
	
	public static final String PROPKEY_IAM_USER_ACCESS_KEY_ID = "sdk.user.aws.accessKeyId";
	public static final String PROPKEY_IAM_USER_SECRET_KEY = "sdk.user.aws.secretAccessKey";
	public static final String PROPKEY_TARGET_AWS_REGION_NAME = "aws.target.region";

	private Properties props;
	
	private synchronized Properties loadPropertiesFromResourceFile(String resourceFileName) throws IOException {
		if (this.props != null) {
			return this.props;
		}
		try (
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
		) {
			if (is == null) {
				throw new IllegalArgumentException("Unable to load classpath resource file: " + resourceFileName);
			}
			Properties props = new Properties();
			props.load(is);
			String iamUserAccessKeyId = props.getProperty(PROPKEY_IAM_USER_ACCESS_KEY_ID);
			if (iamUserAccessKeyId == null) {
				throw new IllegalArgumentException(
						"Properties file missing key: [" + PROPKEY_IAM_USER_ACCESS_KEY_ID + "]");
			}
			String iamUserSecretKey = props.getProperty(PROPKEY_IAM_USER_SECRET_KEY);
			if (iamUserSecretKey == null) {
				throw new IllegalArgumentException(
						"Properties file missing key: [" + PROPKEY_IAM_USER_SECRET_KEY + "]");
			}
			String targetAwsRegionName = props.getProperty(PROPKEY_TARGET_AWS_REGION_NAME);
			if (targetAwsRegionName == null) {
				throw new IllegalArgumentException(
						"Properties file missing key: [" + PROPKEY_TARGET_AWS_REGION_NAME + "]");
			}
			Region targetAwsRegion = Region.of(targetAwsRegionName);
			if (targetAwsRegion == null) {
				throw new IllegalArgumentException(
						"Invalid AWS region name: [" + targetAwsRegionName + "]");
			}
			this.props = props;
			return this.props;
		}
	}

	public Properties getAppProperties() {
		try {
			return loadPropertiesFromResourceFile(PROPERTIES_RESOURCE_FILE_NAME);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load properties resource file", e);
		}
	}
	
	public Region getTargetRegion() {
		try {
			Properties props = loadPropertiesFromResourceFile(PROPERTIES_RESOURCE_FILE_NAME);
			String targetAwsRegionName = props.getProperty(PROPKEY_TARGET_AWS_REGION_NAME);
			return Region.of(targetAwsRegionName);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load properties resource file", e);
		}
	}

	public void setSystemPropertiesFromAppProperties() {
		try {
			Properties props = loadPropertiesFromResourceFile(PROPERTIES_RESOURCE_FILE_NAME);
			System.setProperty(SYSPROPKEY_AWS_ACCESS_KEY_ID, props.getProperty(PROPKEY_IAM_USER_ACCESS_KEY_ID));
			System.setProperty(SYSPROPKEY_AWS_SECRET_ACCESS_KEY, props.getProperty(PROPKEY_IAM_USER_SECRET_KEY));
		} catch (IOException e) {
			throw new RuntimeException("Unable to load properties resource file", e);
		}
	}

	public void clearSystemPropertiesSetFromAppProperties() {
		System.clearProperty(SYSPROPKEY_AWS_ACCESS_KEY_ID);
		System.clearProperty(SYSPROPKEY_AWS_SECRET_ACCESS_KEY);
	}
	
}
