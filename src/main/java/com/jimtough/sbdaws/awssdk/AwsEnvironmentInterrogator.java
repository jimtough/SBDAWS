package com.jimtough.sbdaws.awssdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jimtough.sbdaws.ConfigurationBean;

import software.amazon.awssdk.regions.Region;
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
 * Uses the AWS SDK to get information about the environment that is running this app instance.
 * 
 * Note that the AWS SDK relies on AWS IAM user credentials being defined in system variables
 * (or another documented AWS SDK credential scheme) at runtime.
 * 
 * @author JTOUGH
 */
@Component
public class AwsEnvironmentInterrogator {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentInterrogator.class);

	private final ConfigurationBean configurationBean;
	
	@Autowired
	public AwsEnvironmentInterrogator(ConfigurationBean configurationBean) {
		if (configurationBean == null) {
			throw new IllegalArgumentException("configurationBean cannot be null");
		}
		this.configurationBean = configurationBean;
	}
	
	/**
	 * Retrieve details on the AWS IAM user that is being used by this application when calling the AWS SDK.
	 * 
	 * Note that the AWS SDK relies on AWS IAM user credentials being defined in system variables
	 * (or another documented AWS SDK credential scheme) at runtime.
	 * 
	 * @return User
	 * @throws AwsSdkException Thrown if the SDK call throws an exception
	 */
	public User getIAMUser() throws AwsSdkException {
		try (
			// This only seems to work if AWS_GLOBAL is used as the region
			IAMClient iamClient = IAMClient.builder().region(Region.AWS_GLOBAL).build()
		) {
			// The IAM users are considered "Global" in AWS, rather than region-specific
			GetUserResponse response = iamClient.getUser(GetUserRequest.builder().build());
			User user = response.user();
			LOGGER.debug("IAM user information retrieved successfully | username: [{}]", user.userName());
			return user;
		} catch (Exception e) {
			throw new AwsSdkException("Unable to retrieve information on IAM user", e);
		}
	}

	/**
	 * Retrieves list of ECS clusters owned by this AWS account
	 * 
	 * @return Non-null (possibly empty) list
	 * @throws AwsSdkException Thrown if the SDK call throws an exception
	 */
	public List<Cluster> getECSClusterList() throws AwsSdkException {
		try (
			ECSClient ecsClient = ECSClient.builder().region(this.configurationBean.getAwsTargetRegion()).build();
		) {
			ListClustersResponse response = ecsClient.listClusters(ListClustersRequest.builder().build());
			List<String> clusterArns = response.clusterArns();
			if (clusterArns == null) {
				return Collections.emptyList();
			}
			List<Cluster> clusterList = new ArrayList<>(clusterArns.size());
			LOGGER.debug("My AWS account has {} ECS clusters defined", clusterArns.size());
			for (String clusterArn : clusterArns) {
				LOGGER.debug(" --> clusterArn: {}", clusterArn);
				DescribeClustersResponse descResponse = 
						ecsClient.describeClusters(
								DescribeClustersRequest.builder().clusters(clusterArn).build());
				List<Cluster> clusters = descResponse.clusters();
				// Assume that a describe request with a valid 'arn' will return exactly one result
				Cluster cluster = clusters.get(0);
				LOGGER.debug("   DETAILS | name: {} | arn: {} | containers: {} | services: {} | tasks: {}",
						cluster.clusterName(),
						cluster.clusterArn(),
						cluster.registeredContainerInstancesCount(),
						cluster.activeServicesCount(),
						cluster.runningTasksCount());
				clusterList.add(cluster);
			}
			return clusterList;
		} catch (Exception e) {
			throw new AwsSdkException("Unable to retrieve information on ECS clusters", e);
		}
	}
	
	/**
	 * Retrieves list of S3 buckets owned by this AWS account in any AWS region globally
	 * 
	 * @return Non-null (possibly empty) list
	 * @throws AwsSdkException Thrown if the SDK call throws an exception
	 */
	public List<Bucket> getS3BucketList() throws AwsSdkException {
		try (
			S3Client s3Client = S3Client.builder().region(this.configurationBean.getAwsTargetRegion()).build();
		) {
			// The S3 buckets are considered "Global" in AWS when querying for the list
			ListBucketsResponse response = s3Client.listBuckets(ListBucketsRequest.builder().build());

			List<Bucket> myBucketList = response.buckets();
			if (myBucketList == null) {
				return Collections.emptyList();
			}
			LOGGER.debug("S3 buckets information retrieved successfully | Number of buckets: [{}]", myBucketList.size());
			return myBucketList;
		} catch (Exception e) {
			throw new AwsSdkException("Unable to retrieve information on S3 buckets", e);
		}
	}
	
}
