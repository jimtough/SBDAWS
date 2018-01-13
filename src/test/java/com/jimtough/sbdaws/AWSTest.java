package com.jimtough.sbdaws;

import static org.junit.Assert.*;
import static com.jimtough.sbdaws.ConfigurationBean.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ecs.ECSClient;
import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.ecs.model.DescribeClustersRequest;
import software.amazon.awssdk.services.ecs.model.DescribeClustersResponse;
import software.amazon.awssdk.services.ecs.model.ListClustersRequest;
import software.amazon.awssdk.services.ecs.model.ListClustersResponse;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.GetUserRequest;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

/**
 * Test ability to connect to AWS with IAM credentials and run some simple AWS SDK commands
 * 
 * @author JTOUGH
 */
public class AWSTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(AWSTest.class);

	private static final String PROPERTIES_RESOURCE_FILE_NAME = "app.properties";
	
	private static final String PROPKEY_IAM_USER_ACCESS_KEY_ID = "sdk.user.aws.accessKeyId";
	private static final String PROPKEY_IAM_USER_SECRET_KEY = "sdk.user.aws.secretAccessKey";
	private static final String PROPKEY_TARGET_AWS_REGION_NAME = "aws.target.region";
	
	private static Region targetAwsRegion;
	
	@BeforeClass
	public static void oneTimeSetUp() throws IOException {
		try (
			InputStream is = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(PROPERTIES_RESOURCE_FILE_NAME);
		) {
			assertNotNull("Unable to read properties file as classpath resource", is);
			Properties props = new Properties();
			props.load(is);
			String iamUserAccessKeyId = props.getProperty(PROPKEY_IAM_USER_ACCESS_KEY_ID);
			assertNotNull("Properties file missing key: [" + PROPKEY_IAM_USER_ACCESS_KEY_ID + "]", iamUserAccessKeyId);
			LOGGER.debug("iamUserAccessKeyId: [{}]", iamUserAccessKeyId);
			String iamUserSecretKey = props.getProperty(PROPKEY_IAM_USER_SECRET_KEY);
			assertNotNull("Properties file missing key: [" + PROPKEY_IAM_USER_SECRET_KEY + "]", iamUserSecretKey);
			// Uncomment this if you want your secret IAM access key logged
			//LOGGER.debug("iamUserSecretKey: [{}]", iamUserSecretKey);
			String targetAwsRegionName = props.getProperty(PROPKEY_TARGET_AWS_REGION_NAME);
			assertNotNull("Properties file missing key: [" + PROPKEY_TARGET_AWS_REGION_NAME + "]", targetAwsRegionName);
			targetAwsRegion = Region.of(targetAwsRegionName);
			assertNotNull("Invalid target region name: [" + targetAwsRegionName + "]", targetAwsRegion);
			LOGGER.debug("targetAwsRegion: [{}]", targetAwsRegion.value());
			
			System.setProperty(SYSPROPKEY_AWS_ACCESS_KEY_ID, iamUserAccessKeyId);
			System.setProperty(SYSPROPKEY_AWS_SECRET_ACCESS_KEY, iamUserSecretKey);
		}
	}

	@AfterClass
	public static void oneTimeTearDown() {
		System.clearProperty(SYSPROPKEY_AWS_ACCESS_KEY_ID);
		System.clearProperty(SYSPROPKEY_AWS_SECRET_ACCESS_KEY);
	}

	// No setup/cleanup required for individual tests right now
	@Before public void setUp() throws Exception {}
	@After public void tearDown() throws Exception {}
	
	@Test
	public void testReadPropertiesResourceFile() throws Exception {
		// Nothing to test - just need @BeforeClass method to execute with no exceptions
	}

	@Test
	public void testS3Client() throws Exception {
		try (
			S3Client s3Client = S3Client.builder()
				.region(targetAwsRegion)
				.build();
		) {
			assertNotNull(s3Client);
			
			ListBucketsResponse response = s3Client.listBuckets(ListBucketsRequest.builder().build());
			
			assertNotNull(response);
			List<Bucket> myBucketList = response.buckets();
			assertNotNull(myBucketList);
			LOGGER.info("My AWS account has {} S3 buckets", myBucketList.size());
			for (Bucket bucket : myBucketList) {
				LOGGER.info(" --> bucket: {}", bucket.name());
			}
		}
	}

	@Test
	public void testIAMClient() throws Exception {
		try (
			IAMClient iamClient = IAMClient.builder()
				// The IAM SDK must use the "Global" region
				.region(Region.AWS_GLOBAL)
				.build();
		) {
			assertNotNull(iamClient);
			
			GetUserResponse response = iamClient.getUser(GetUserRequest.builder().build());
			
			assertNotNull(response);
			User currentUser = response.user();
			assertNotNull(currentUser);
			LOGGER.info("current IAM user | userName: {} | userId: {} | arn: {}",
					currentUser.userName(), currentUser.userId(), currentUser.arn());
		}
	}

	@Test
	public void testEc2Client() throws Exception {
		try (
			EC2Client ec2Client = EC2Client.builder()
				.region(targetAwsRegion)
				.build();
		) {
			assertNotNull(ec2Client);
			
			DescribeInstanceStatusResponse response = 
					ec2Client.describeInstanceStatus(DescribeInstanceStatusRequest.builder().build());
			
			assertNotNull(response);
			List<InstanceStatus> instanceStatuses = response.instanceStatuses();
			assertNotNull(instanceStatuses);
			LOGGER.info("My AWS account has {} running EC2 instances", instanceStatuses.size());
			for (InstanceStatus instanceStatus : instanceStatuses) {
				LOGGER.info(" --> EC2 instance | AZ: {} | id: {} | state: {} | status: {}",
						instanceStatus.availabilityZone(), 
						instanceStatus.instanceId(), 
						instanceStatus.instanceState().name(),
						instanceStatus.instanceStatus().status());
			}
		}
	}

	@Test
	public void testEcsClient() throws Exception {
		try (
			ECSClient ecsClient = ECSClient.builder()
				.region(targetAwsRegion)
				.build();
		) {
			assertNotNull(ecsClient);
			
			ListClustersResponse response = ecsClient.listClusters(ListClustersRequest.builder().build());
			
			assertNotNull(response);
			List<String> clusterArns = response.clusterArns();
			assertNotNull(clusterArns);
			LOGGER.info("My AWS account has {} ECS clusters defined", clusterArns.size());
			for (String clusterArn : clusterArns) {
				LOGGER.info(" --> clusterArn: {}", clusterArn);
				DescribeClustersResponse descResponse = 
						ecsClient.describeClusters(
								DescribeClustersRequest.builder().clusters(clusterArn).build());
				List<Cluster> clusters = descResponse.clusters();
				assertEquals(1, clusters.size());
				Cluster cluster = clusters.get(0);
				LOGGER.info("   DETAILS | name: {} | registered containers: {} | running tasks: {}",
						cluster.clusterName(),
						cluster.registeredContainerInstancesCount(),
						cluster.runningTasksCount());
			}
		}
	}
	
}


