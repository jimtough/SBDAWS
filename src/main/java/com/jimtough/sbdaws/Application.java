package com.jimtough.sbdaws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

	private static final String AWS_EFS_VOLUME_MOUNT_PATH_STRING = "/datafiles";
	private static final Path AWS_EFS_VOLUME_MOUNT_PATH = Paths.get("/datafiles");
	private static final String FILE_TO_TOUCH_FULL_PATH_STRING = AWS_EFS_VOLUME_MOUNT_PATH_STRING + "/touchme.txt";
	//private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	
	private final String EOL = System.lineSeparator();
	private final String HTML_HELLO_MESSAGE = "<h2>Hello! This is the Spring Boot webapp that %s created.</h2>";

	private final String HTML_REGION_HEADER = "<h3>This app is running in region [<b>%s</b>]</h3>";
	
	private final String HTML_IAM_USER_HEADER = "<h3>My IAM User: [<b>%s</b>]</h3>";
	private final String HTML_IAM_USER_DETAILS = 
			"<ul>" + EOL +
				"<li><b>IAM user creation date:</b> %s</li>" + EOL +
			"</ul>";
	private final String HTML_IAM_USER_SDK_FAILURE = "<h3><b>SDK request for IAM user details failed!</b></h3>";
	
	private final String HTML_ECS_CLUSTER_HEADER = "<h3>My ECS clusters (%d in total)</h3>";
	private final String HTML_ECS_CLUSTER_DETAILS =
			"<li><b>name:</b> %s | <b>containers:</b> %s | <b>services:</b> %s | <b>tasks:</b> %s</li>";
	private final String HTML_ECS_CLUSTER_SDK_FAILURE = "<h3><b>SDK request for ECS cluster details failed!</b></h3>";
	
	private final String HTML_S3_BUCKETS_HEADER = "<h3>My S3 buckets (%d in total)</h3>";
	private final String HTML_S3_BUCKET_DETAILS = "<li><b>name:</b> %s | <b>creation date:</b> %s</li>";
	private final String HTML_S3_BUCKETS_SDK_FAILURE = "<h3><b>SDK request for S3 buckets list failed!</b></h3>";

	private final String HTML_EFS_VOLUME_FILES_HEADER = "<h3>Files in my EFS volume (%d in total)</h3>";
	private final String HTML_EFS_VOLUME_FILE_DETAILS = "<li>"
			+ "<b>file path:</b> [%s] | "
			+ "<b>created: </b> %s | "
			+ "<b>last modified:</b> %s | "
			+ "<b>size: </b> %s | "
			+ "<b>is directory?: </b> %b"
			+ "</li>";
	
	@Autowired
	private ConfigurationBean configurationBean;
	
	@Autowired
	private AwsEnvironmentInterrogator interrogator;

	// This is the only HTTP request handler defined in the application.
	// It will return a kind-of-ugly HTML reply with details from the AWS SDK method call responses.
	@RequestMapping("/")
	public String home() throws Exception {
		LOGGER.info("Request received");
		touchMe();
		
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		
		// Start with a simple greeting that includes one of the values from the properties file.
		// This should insert YOUR name if you edited the "maven-filter-values.properties" file.
		sb.append(String.format(HTML_HELLO_MESSAGE, configurationBean.getMyName()));
		
		sb.append(String.format(HTML_REGION_HEADER, configurationBean.getAwsTargetRegion().value()));

		// Add the IAM user details to the HTML response
		try {
			User user = interrogator.getIAMUser();
			sb.append(String.format(HTML_IAM_USER_HEADER, user != null ? user.userName() : "UNKNOWN"));
			if (user != null) {
				sb.append(String.format(HTML_IAM_USER_DETAILS, user.createDate()));
			}
		} catch (AwsSdkException e) {
			sb.append(HTML_IAM_USER_SDK_FAILURE);
		}

		// Add the ECS cluster details to the HTML response
		try {
			List<Cluster> clusterList = interrogator.getECSClusterList();
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
			List<Bucket> bucketList = interrogator.getS3BucketList();
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
		
		List<Path> efsVolumeMountDirContents = listFilesInEFSVolumeMountDir();
		sb.append(String.format(HTML_EFS_VOLUME_FILES_HEADER, efsVolumeMountDirContents.size()));
		if (!efsVolumeMountDirContents.isEmpty()) {
			sb.append("<ul>").append(EOL);
			for (Path p : efsVolumeMountDirContents) {
				sb.append(String.format(HTML_EFS_VOLUME_FILE_DETAILS, 
						p.toString(),
						Files.getAttribute(p, "creationTime", LinkOption.NOFOLLOW_LINKS),
						Files.getAttribute(p, "lastModifiedTime"),
						Files.getAttribute(p, "size"),
						Files.getAttribute(p, "isDirectory")))
					.append(EOL);
			}
			sb.append("</ul>").append(EOL);
		}

		sb.append("<p>Visit me here: <a href='http://blog.jimtough.com'>http://blog.jimtough.com</a></p>");
		
		sb.append("</body></html>");
		
		return sb.toString();
	}

	public void touchMe() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		if (isWindows) {
			LOGGER.warn("This is Windows?!?");
		} else {
			LOGGER.debug("System property 'os.name': [{}]", System.getProperty("os.name"));
		}
		try {
			String unixTouchCommand = "touch -a -m " + FILE_TO_TOUCH_FULL_PATH_STRING;
			LOGGER.debug("Executing O/S command: [{}]", unixTouchCommand);
			Process process = Runtime.getRuntime().exec(String.format(unixTouchCommand));
			process.waitFor(10, TimeUnit.SECONDS);
			LOGGER.debug("Command executed");
		} catch (IOException e) {
			LOGGER.error("Unable to 'touch' file", e);
		} catch (InterruptedException ie) {
			LOGGER.warn("Interrupted while touching...");
		}
	}

	public List<Path> listFilesInEFSVolumeMountDir() {
		try (Stream<Path> pathStream = Files.walk(AWS_EFS_VOLUME_MOUNT_PATH)) {
			List<Path> contentsOfEFSVolumeMountDir = new ArrayList<>();
			// Apply some stream operations
			pathStream
				.map(p->p.toAbsolutePath())
				.sorted()
				.forEach(p -> contentsOfEFSVolumeMountDir.add(p));
			return contentsOfEFSVolumeMountDir;
		} catch (IOException e) {
			LOGGER.error("Exception while walking dir tree from root dir: [{}]", AWS_EFS_VOLUME_MOUNT_PATH_STRING, e);
			return Collections.emptyList();
		}
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
