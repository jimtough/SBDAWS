package com.jimtough.sbdaws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jimtough.sbdaws.awssdk.AwsEnvironmentInterrogator;
import com.jimtough.sbdaws.awssdk.AwsSdkException;

import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.model.Bucket;

@SpringBootApplication
@RestController
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
	
	private final String EOL = System.lineSeparator();
	private final String HTML_HELLO_MESSAGE = "<h2>Hello! This is the Spring Boot webapp that %s created.</h2>";
	
	private final String HTML_IAM_USER_HEADER = "<h3>My IAM User: <b>%s</b></h3>";
	private final String HTML_IAM_USER_DETAILS = 
			"<ul>" + EOL +
				"<li><b>arn:</b> %s</li>" + EOL +
				"<li><b>creation date:</b> %s</li>" + EOL +
			"</ul>";
	private final String HTML_IAM_USER_SDK_FAILURE = "<h3><b>SDK request for IAM user details failed!</b></h3>";
	
	private final String HTML_ECS_CLUSTER_HEADER = "<h3>My ECS clusters (%d in total)</h3>";
	private final String HTML_ECS_CLUSTER_DETAILS =
			"<li><b>name:</b> %s | <b>containers:</b> %s | <b>services:</b> %s | <b>tasks:</b> %s</li>";
	private final String HTML_ECS_CLUSTER_SDK_FAILURE = "<h3><b>SDK request for ECS cluster details failed!</b></h3>";
	
	private final String HTML_S3_BUCKETS_HEADER = "<h3>My S3 buckets (%d in total)</h3>";
	private final String HTML_S3_BUCKET_DETAILS = "<li><b>name:</b> %s | <b>creation date:</b> %s</li>";
	private final String HTML_S3_BUCKETS_SDK_FAILURE = "<h3><b>SDK request for S3 buckets list failed!</b></h3>";
	
	@Autowired
	private ConfigurationBean configurationBean;
	
	@Autowired
	private AwsEnvironmentInterrogator interrogator;

	// This is the only HTTP request handler defined in the application.
	// It will return a kind-of-ugly HTML reply with details from the AWS SDK method call responses.
	@RequestMapping("/")
	public String home() {
		LOGGER.info("Request received");
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		
		// Start with a simple greeting that includes one of the values from the properties file.
		// This should insert YOUR name if you edited the "maven-filter-values.properties" file.
		sb.append(String.format(HTML_HELLO_MESSAGE, configurationBean.getMyName()));

		// Add the IAM user details to the HTML response
		try {
			User user = interrogator.getIAMUser();
			sb.append(String.format(HTML_IAM_USER_HEADER, user != null ? user.userName() : "UNKNOWN"));
			if (user != null) {
				sb.append(String.format(HTML_IAM_USER_DETAILS, user.arn(), user.createDate()));
			}
		} catch (AwsSdkException e) {
			sb.append(HTML_IAM_USER_SDK_FAILURE);
		}

		// Add the ECS cluster details to the HTML response
		try {
			List<Cluster> clusterList = interrogator.getECSClusterList(configurationBean.getAwsTargetRegion());
			sb.append(String.format(HTML_ECS_CLUSTER_HEADER, clusterList.size()));
			if (!clusterList.isEmpty()) {
				sb.append("<ul>").append(EOL);
				for (Cluster cluster : clusterList) {
					sb.append(String.format(HTML_ECS_CLUSTER_DETAILS, 
							cluster.clusterName(),
							cluster.registeredContainerInstancesCount(),
							cluster.activeServicesCount(),
							cluster.runningTasksCount())
						).append(EOL);
				}
				sb.append("</ul>").append(EOL);
			}
		} catch (AwsSdkException e) {
			sb.append(HTML_ECS_CLUSTER_SDK_FAILURE);
		}

		// Add the S3 bucket details to the HTML response
		try {
			List<Bucket> bucketList = interrogator.getS3BucketList(configurationBean.getAwsTargetRegion());
			sb.append(String.format(HTML_S3_BUCKETS_HEADER, bucketList.size()));
			if (!bucketList.isEmpty()) {
				sb.append("<ul>").append(EOL);
				for (Bucket bucket : bucketList) {
					sb.append(String.format(HTML_S3_BUCKET_DETAILS, bucket.name(), bucket.creationDate())).append(EOL);
				}
				sb.append("</ul>").append(EOL);
			}
		} catch (AwsSdkException e) {
			sb.append(HTML_S3_BUCKETS_SDK_FAILURE);
		}
		
		sb.append("</body></html>");
		return sb.toString();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
