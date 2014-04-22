package co.codewizards.cloudstore.droid;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import co.codewizards.cloudstore.core.util.ZipUtil;
import android.content.Context;
import dalvik.system.DexClassLoader;

public class DexJarClassLoader extends DexClassLoader {

	private static final ThreadLocal<File> extractedDexJarDirThreadLocal = new ThreadLocal<>();
	private final File extractedDexJarDir;

	public DexJarClassLoader(final Context appContext, final String dexJarAssetName, final ClassLoader parent) {
		super(getDexPath(appContext, dexJarAssetName), 
				getOptimizedDirectory(appContext, dexJarAssetName), 
				null, parent);
		
		extractedDexJarDir = extractedDexJarDirThreadLocal.get();
		extractedDexJarDirThreadLocal.remove();
		
		if (extractedDexJarDir == null)
			throw new IllegalStateException("extractedDexJarDir == null");
	}

	@Override
	protected URL findResource(String name) {
		System.out.println("findResource: name=" + name);
		
		final URL localResource = _findResource(name);
		if (localResource != null)
			return localResource;
			
		final URL resource = super.findResource(name);
		return resource;
	}

	private URL _findResource(String name) {
		final File candidate = new File(extractedDexJarDir, name);
		if (candidate.isFile()) {
			try {
				return candidate.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) {
		System.out.println("findResources: name=" + name);
		final Enumeration<URL> resources = super.findResources(name);

		final URL localResource = _findResource(name);
		if (localResource == null)
			return resources;

		final List<URL> result = new ArrayList<>(1);
		result.add(localResource);
		while (resources.hasMoreElements())
			result.add(resources.nextElement());

		return new EnumerationOfIterator<>(result.iterator());
	}

	private static class EnumerationOfIterator<E> implements Enumeration<E> {
		private final Iterator<E> iterator;
		
		public EnumerationOfIterator(Iterator<E> iterator) {
			if (iterator == null)
				throw new IllegalArgumentException("iterator == null");
			
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		@Override
		public E nextElement() {
			return iterator.next();
		}
	}

	private static String getDexPath(final Context appContext, final String dexJarAssetName) {
		final File extractedDexJarDir = createExtractedDexJarDir(appContext, dexJarAssetName);
		final File dexFile = new File(extractedDexJarDir, "classes.dex");
		if (!dexFile.isFile())
			throw new IllegalStateException("DEX file not found: " + dexFile.getAbsolutePath());
		
		return dexFile.getAbsolutePath();
	}

	private static File createExtractedDexJarDir(final Context appContext, final String dexJarAssetName) {
		final File resDir = appContext.getDir(getDexJarAssetNameWithoutExtension(dexJarAssetName) + ".dir", Context.MODE_PRIVATE);
		// TODO delete all contents recursively to make sure no old garbage is lingering around!
		// TODO extract asset directly without first copying it to a file!
		final File tmpFile = createFileFromAsset(appContext, "tmp", dexJarAssetName);
		try {
			ZipUtil.unzipArchive(tmpFile, resDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			tmpFile.delete();
		}
		extractedDexJarDirThreadLocal.set(resDir);
		return resDir;
	}

	private static String getOptimizedDirectory(final Context appContext, final String dexJarAssetName) {
		final File dir = appContext.getDir(getDexJarAssetNameWithoutExtension(dexJarAssetName) + ".odex", Context.MODE_PRIVATE);
		return dir.getAbsolutePath();
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

		final int BUF_SIZE = 8 * 1024;
		try {
			final InputStream in = new BufferedInputStream(appContext.getAssets().open(assetName));
			final OutputStream out = new BufferedOutputStream(new FileOutputStream(dexFile));
			final byte[] buf = new byte[BUF_SIZE];
			int len;
			while((len = in.read(buf, 0, BUF_SIZE)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
		
		return dexFile;
	}


}
