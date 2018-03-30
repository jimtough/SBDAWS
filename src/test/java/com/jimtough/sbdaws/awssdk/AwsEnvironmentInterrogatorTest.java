package com.jimtough.sbdaws.awssdk;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jimtough.sbdaws.ConfigurationBean;
import com.jimtough.sbdaws.PropertiesLoader;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.model.Bucket;

/**
 * JUnit integration test for the {@code AwsEnvironmentInterrogator} class
 * 
 * @author JTOUGH
 */
public class AwsEnvironmentInterrogatorTest {
	
	private static final PropertiesLoader PROPERTIES_LOADER = new PropertiesLoader();
	private static Region targetAwsRegion;
	
	@BeforeClass
	public static void oneTimeSetUp() throws IOException {
		PROPERTIES_LOADER.getAppProperties();
		PROPERTIES_LOADER.setSystemPropertiesFromAppProperties();
		targetAwsRegion = PROPERTIES_LOADER.getTargetRegion();
	}

	@AfterClass
	public static void oneTimeTearDown() {
		PROPERTIES_LOADER.clearSystemPropertiesSetFromAppProperties();
	}

	private ConfigurationBean cbMock;
	private AwsEnvironmentInterrogator interrogator;
	
	@Before public void setUp() throws Exception {
		cbMock = mock(ConfigurationBean.class);
		when(cbMock.getAwsTargetRegion()).thenReturn(targetAwsRegion);
		interrogator = new AwsEnvironmentInterrogator(cbMock);
	}
	
	@After public void tearDown() throws Exception {
		interrogator = null;
	}

	@Test
	public void testGetIAMUser() throws Exception {
		User user = interrogator.getIAMUser();
		assertNotNull(user);
		assertNotNull(user.arn());
	}

	@Test
	public void testGetS3BucketList() throws Exception {
		List<Bucket> bucketList = interrogator.getS3BucketList();
		assertNotNull(bucketList);
	}

	@Test
	public void testGetECSClusterList() throws Exception {
		List<Cluster> clusterList = interrogator.getECSClusterList();
		assertNotNull(clusterList);
	}
	
}
