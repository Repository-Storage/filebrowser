// Copyright 2012 Dragan Sahpaski
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.dragansah.filebrowser.services;

import java.io.File;
import java.io.IOException;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestHandler;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;

import com.dragansah.filebrowser.domain.FileModel;
import com.dragansah.filebrowser.sessionstate.UserInfo;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
public class FileBrowserModule
{
	public static void bind(ServiceBinder binder)
	{
	}

	public static void contributeFactoryDefaults(MappedConfiguration<String, Object> configuration)
	{
		configuration.override(SymbolConstants.APPLICATION_VERSION, "1.0-SNAPSHOT");
		configuration.override(SymbolConstants.PRODUCTION_MODE, false);
	}

	public static void contributeApplicationDefaults(
			MappedConfiguration<String, Object> configuration)
	{
		configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en, MK_mk");
	}

	/**
	 * This is a service definition, the service will be named "TimingFilter". The interface,
	 * RequestFilter, is used within the RequestHandler service pipeline, which is built from the
	 * RequestHandler service configuration. Tapestry IoC is responsible for passing in an
	 * appropriate Logger instance. Requests for static resources are handled at a higher level, so
	 * this filter will only be invoked for Tapestry related requests.
	 * <p/>
	 * <p/>
	 * Service builder methods are useful when the implementation is inline as an inner class (as
	 * here) or require some other kind of special initialization. In most cases, use the static
	 * bind() method instead.
	 * <p/>
	 * <p/>
	 * If this method was named "build", then the service id would be taken from the service
	 * interface and would be "RequestFilter". Since Tapestry already defines a service named
	 * "RequestFilter" we use an explicit service id that we can reference inside the contribution
	 * method.
	 */
	public RequestFilter buildTimingFilter(final Logger log)
	{
		return new RequestFilter()
		{
			public boolean service(Request request, Response response, RequestHandler handler)
					throws IOException
			{
				long startTime = System.currentTimeMillis();

				try
				{
					// The responsibility of a filter is to invoke the corresponding method
					// in the handler. When you chain multiple filters together, each filter
					// received a handler that is a bridge to the next filter.

					return handler.service(request, response);
				}
				finally
				{
					long elapsed = System.currentTimeMillis() - startTime;

					log.info(String.format("Request time: %d ms", elapsed));
				}
			}
		};
	}

	/**
	 * This is a contribution to the RequestHandler service configuration. This is how we extend
	 * Tapestry using the timing filter. A common use for this kind of filter is transaction
	 * management or security. The @Local annotation selects the desired service by type, but only
	 * from the same module. Without @Local, there would be an error due to the other service(s)
	 * that implement RequestFilter (defined in other modules).
	 */
	public void contributeRequestHandler(OrderedConfiguration<RequestFilter> configuration,
			@Local RequestFilter filter)
	{
		// Each contribution to an ordered configuration has a name, When necessary, you may
		// set constraints to precisely control the invocation order of the contributed filter
		// within the pipeline.

		configuration.add("Timing", filter);
	}

	/**
	 * Contribute a {@link ValueEncoder} for {@link FileModel}.
	 */
	public static void contributeValueEncoderSource(
			MappedConfiguration<Class<?>, ValueEncoder<?>> conf, final ApplicationStateManager asm)
	{
		conf.add(FileModel.class, new ValueEncoder<FileModel>()
		{
			public String toClient(FileModel value)
			{
				return protectFile(asm, new File(value.getAbsolutePath()));
			}

			public FileModel toValue(String path)
			{
				return FileModel.fromFile(reconstructProtectedFileFromPath(asm, path));
			}
		});

		conf.add(File.class, new ValueEncoder<File>()
		{
			public String toClient(File value)
			{
				return protectFile(asm, value);
			}

			public File toValue(String path)
			{
				return reconstructProtectedFileFromPath(asm, path);
			}
		});
	}

	/**
	 * Protect the given {@link File} from security attacks on the client side by stripping the root
	 * folder path of the user folder (in the file system) from {@link File#getAbsolutePath()}.
	 * <p>
	 * The {@link ApplicationStateManager} is needed for obtaining the {@link UserInfo}
	 * {@link SessionState} object.
	 * 
	 * @param asm
	 * 
	 * @param value
	 *            The file that needs to be encoded i.e. stripped.
	 * 
	 * @return The stripped {@link File#getAbsolutePath()}
	 * 
	 * @see FileBrowserModule#reconstructProtectedFileFromPath(ApplicationStateManager, String)
	 */
	private static String protectFile(final ApplicationStateManager asm, File value)
	{
		String rootFolder = asm.get(UserInfo.class).getRootFolderForLoggedInUser();
		return value.getAbsolutePath().replace(rootFolder, "");
	}

	/**
	 * Reconstruct the protected {@link File} by adding the root folder path of the user folder (in
	 * the file system) to the path obtained from the client side.
	 * <p>
	 * The {@link ApplicationStateManager} is needed for obtaining the {@link UserInfo}
	 * {@link SessionState} object.
	 * 
	 * @param asm
	 * 
	 * @param path
	 *            The file path obtained from the client side
	 * 
	 * @return The reconstructed {@link File}
	 * 
	 * @see FileBrowserModule#protectFile(ApplicationStateManager, File)
	 */
	private static File reconstructProtectedFileFromPath(final ApplicationStateManager asm,
			String path)
	{
		String rootFolder = asm.get(UserInfo.class).getRootFolderForLoggedInUser();
		return new File(rootFolder + path);
	}
}
