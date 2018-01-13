package com.jimtough.sbdaws;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import software.amazon.awssdk.regions.Region;

@Configuration
@PropertySource("classpath:/app.properties")
public class ConfigurationBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationBean.class);

	/**
	 * The AWS SDK looks for this System property to get the IAM user access key id
	 */
	public static final String SYSPROPKEY_AWS_ACCESS_KEY_ID = "aws.accessKeyId";
	/**
	 * The AWS SDK looks for this System property to get the IAM user secret key
	 */
	public static final String SYSPROPKEY_AWS_SECRET_ACCESS_KEY = "aws.secretAccessKey";

	@Value("${my.name}")
	private String myName;

	@Value("${sdk.user.aws.accessKeyId}")
	private String awsAccessKeyId;

	@Value("${sdk.user.aws.secretAccessKey}")
	private String awsSecretAccessKey;

	@Value("${aws.target.region}")
	private String awsTargetRegionName;

	private Region awsTargetRegion;
	
	@PostConstruct
	void postConstruct() {
		awsTargetRegion = Region.of(awsTargetRegionName);
		LOGGER.debug("App configuration properties loaded | AWS target region: [{}] | IAM user id: [{}]",
				this.awsTargetRegion.value(), this.awsAccessKeyId);
		System.setProperty(SYSPROPKEY_AWS_ACCESS_KEY_ID, awsAccessKeyId);
		System.setProperty(SYSPROPKEY_AWS_SECRET_ACCESS_KEY, awsSecretAccessKey);
	}
	
	public String getMyName() {
		return myName;
	}

	public Region getAwsTargetRegion() {
		return awsTargetRegion;
	}
	
}
