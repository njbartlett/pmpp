/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.s3install;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3Object;
import com.threerings.s3.client.S3ObjectEntry;
import com.threerings.s3.client.S3ObjectListing;

/**
 * @author Neil Bartlett
 *
 */
public class S3Scanner implements Runnable {
	
	private final Logger log = LoggerFactory.getLogger(S3Scanner.class);
	
	private final BundleContext osgi;
	private final S3Connection s3;
	private final String bucket;
	private final String prefix;
	
	public S3Scanner(BundleContext osgi, S3Connection s3, String bucket, String prefix) {
		this.osgi = osgi;
		this.s3 = s3;
		this.bucket = bucket;
		this.prefix = prefix;
	}

	public void run() {
		// Read files into map
		log.info("Scanning bucket " + bucket);
		Map<String,S3ObjectEntry> objectEntries = new HashMap<String, S3ObjectEntry>();
		try {
			S3ObjectListing listing = s3.listObjects(bucket, prefix, null, 0, null);
			addListingToMap(bucket, objectEntries, listing);
			
			while(listing.truncated()) {
				log.debug("Listing was truncated, retrieving further entries");
				listing = s3.listObjects(bucket, prefix, listing.getNextMarker(), 0, null);
				addListingToMap(bucket, objectEntries, listing);
			}
		} catch (S3Exception e1) {
			e1.printStackTrace();
			return;
		}
		
		// Calculate which bundles are managed by this scanner instance
		String uriPrefix = getBucketURIPrefix(bucket) + prefix;
		Bundle[] bundles = osgi.getBundles();
		Map<String,Bundle> bundleMap = new HashMap<String, Bundle>();
		for (Bundle bundle : bundles) {
			if(bundle.getLocation().startsWith(uriPrefix)) {
				bundleMap.put(bundle.getLocation(), bundle);
			}
		}

		// Remove and update bundles
		for(String location : bundleMap.keySet()) {
			Bundle bundle = bundleMap.get(location);
			S3ObjectEntry entry = objectEntries.get(location);
			
			if(entry == null) {
				// Remove bundle
				try {
					log.info("UNINSTALLING BUNDLE: " + location);
					bundle.uninstall();
				} catch (BundleException e) {
					log.error("Error uninstalling bundle", e);
				}
			} else if(entry.getLastModified().getTime() > bundle.getLastModified()) {
				// Update bundle
				log.info("UPDATING BUNDLE: " + location);
				try {
					S3Object object = s3.getObject(bucket, entry.getKey());
					InputStream stream = object.getInputStream();
					bundle.update(stream);
				} catch (S3Exception e) {
					log.error("Error reading bundle for update", e);
				} catch (BundleException e) {
					log.error("Error updating/starting bundle", e);
				}
			}
		}
		
		// Add missing bundles
		for(String location : objectEntries.keySet()) {
			if(!bundleMap.containsKey(location)) {
				log.info("INSTALLING BUNDLE: " + location);
				S3ObjectEntry entry = objectEntries.get(location);
				try {
					S3Object object = s3.getObject(bucket, entry.getKey());
					Bundle bundle = osgi.installBundle(location, object.getInputStream());
					log.info("Bundle ID is " + bundle.getBundleId());
					bundle.start();
				} catch (S3Exception e) {
					log.error("Error reading bundle for install", e);
				} catch (BundleException e) {
					log.error("Error installing/starting bundle", e);
				}
			}
		}
	}

	/**
	 * @param bucket
	 * @param objectEntries
	 * @param listing
	 */
	private static void addListingToMap(String bucket, Map<String, S3ObjectEntry> objectEntries,
			S3ObjectListing listing) {
		List<S3ObjectEntry> entries = listing.getEntries();
		for (S3ObjectEntry entry : entries) {
			objectEntries.put(getBucketURIPrefix(bucket) + entry.getKey(), entry);
		}
	}
	
	private static String getBucketURIPrefix(String bucket) {
		return "s3:" + bucket + "/";
	}
	
}
