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

package com.dragansah.filebrowser.components;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Session;

import com.dragansah.filebrowser.Constants;
import com.dragansah.filebrowser.pages.Index;
import com.dragansah.filebrowser.sessionstate.UserInfo;

@Import(stylesheet = "style.css")
public class Layout
{
	@SuppressWarnings("unused")
	@Property
	@Parameter(required = true, defaultPrefix = BindingConstants.LITERAL)
	private String title;

	@Inject
	private PageRenderLinkSource linkSource;

	@Inject
	private Request request;

	@SessionState
	private UserInfo userInfo;

	public Link getIndexPage()
	{
		return linkSource.createPageRenderLink(Index.class);
	}

	public String getAppServer()
	{
		return Constants.APP_SERVER;
	}

	public String getUserName()
	{
		return userInfo.getUsername();
	}

	Object onLogout() throws MalformedURLException
	{
		Session session = request.getSession(false);
		if (session != null)
		{
			session.invalidate();
			userInfo = null;
		}

		return new URL(Constants.CAS_SERVER + "/cas/logout?url=http://" + Constants.APP_SERVER
				+ "/");
	}
}
