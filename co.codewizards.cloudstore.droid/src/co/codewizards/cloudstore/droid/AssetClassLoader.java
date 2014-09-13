package co.codewizards.cloudstore.droid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

import android.content.Context;
import dalvik.system.DexClassLoader;

/**
 * {@link DexClassLoader} loading JARs contained in the application's assets.
 * <p>
 * To be able to load a JAR, the JAR should be located in the "assets" and referenced by its
 * name there. The JAR should contain the resources in their normal locations. The classes,
 * however, should not be located as *.class files in the JAR. Instead, there should be
 * a "classes.dex" in the root of the JAR.
 * <p>
 * This class was inspired by the article
 * <a href="http://android-developers.blogspot.co.il/2011/07/custom-class-loading-in-dalvik.html">Custom Class Loading in Dalvik</a>. 
 * @author Marco หงุ่ยตระกูล-Schulze
 */
public class AssetClassLoader extends DexClassLoader {

	public AssetClassLoader(final Context appContext, final String[] dexJarAssetNames, final ClassLoader parent) {
		super(getDexPath(appContext, dexJarAssetNames), 
				getOptimizedDirectory(appContext, dexJarAssetNames), 
				null, parent);
	}

	@Override
	protected URL findResource(String name) {
		//System.out.println("findResource: name=" + name);
		
		final URL resource = super.findResource(name);
		return resource;
	}

	@Override
	protected Enumeration<URL> findResources(String name) {
		//System.out.println("findResources: name=" + name);
		try {
			final Enumeration<URL> resources = super.findResources(name);
			return resources;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String getDexPath(final Context appContext, final String[] dexJarAssetNames) {
		final StringBuilder path = new StringBuilder();
		for (final String dexJarAssetName : dexJarAssetNames) {
			final File dexJarFile = createFileFromAsset(appContext, 
					getDexJarAssetNameWithoutExtension(dexJarAssetName) + ".lib", dexJarAssetName);
			
			if (path.length() > 0)
				path.append(File.pathSeparatorChar);
			
			path.append(dexJarFile.getAbsolutePath());
		}
		return path.toString();
	}

	private static String getOptimizedDirectory(final Context appContext, final String[] dexJarAssetNames) {
		long hashCode = 0;
		for (String dexJarAssetName : dexJarAssetNames) {
			final String baseName = getDexJarAssetNameWithoutVersionAndExtension(dexJarAssetName);
			hashCode = hashCode * 31 + (Integer.MAX_VALUE + baseName.hashCode());
		}
		final File dir = appContext.getDir("odex_" + Long.toString(Math.abs(hashCode), 36), Context.MODE_PRIVATE);
		return dir.getAbsolutePath();
	}

	private static String getDexJarAssetNameWithoutVersionAndExtension(final String dexJarAssetName) {
		String baseName = getDexJarAssetNameWithoutExtension(dexJarAssetName);
		String version = "";
		while (version.indexOf('.') < 0) {
			int lastMinusIndex = baseName.lastIndexOf('-');
			if (lastMinusIndex < 0)
				break;
			
			version = baseName.substring(lastMinusIndex) + version;
			baseName = baseName.substring(0, lastMinusIndex);
		}
		return baseName;
	}

	private static String getDexJarAssetNameWithoutExtension(final String dexJarAssetName) {
		if (dexJarAssetName == null) 
			throw new NullPointerException("dexJarAssetName");
		
		if (!dexJarAssetName.endsWith(".jar"))
			throw new IllegalArgumentException("dexJarAssetName does not end with '.jar': " + dexJarAssetName);
		
		final String nameWithoutExtension = dexJarAssetName.substring(0, dexJarAssetName.length() - 4);
		return nameWithoutExtension;
	}

	private static File createFileFromAsset(final Context appContext, final String dirName, final String assetName) {
		final File dexFile = new File(appContext.getDir(dirName, Context.MODE_PRIVATE), assetName);
		// TODO try to find out and compare timestamps in order to *not* copy the file if not changed!
		
		try {
			final InputStream in = appContext.getAssets().open(assetName);
			try {
				final OutputStream out = new FileOutputStream(dexFile);
				try {
					transferStreamData(in, out);
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
		
		return dexFile;
	}

	private static void transferStreamData(final InputStream in, final OutputStream out) throws IOException {
		final int BUF_SIZE = 16 * 1024;
		final byte[] buf = new byte[BUF_SIZE];
		int len;
		while((len = in.read(buf, 0, BUF_SIZE)) >= 0) {
			if (len > 0)
				out.write(buf, 0, len);
		}
	}
}
