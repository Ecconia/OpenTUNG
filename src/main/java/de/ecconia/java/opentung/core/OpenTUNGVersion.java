package de.ecconia.java.opentung.core;

import de.ecconia.java.json.JSONObject;
import de.ecconia.java.json.JSONParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class OpenTUNGVersion
{
	private final String gitCommitHash;
	private final String gitBranch;
	private final boolean gitDirty;
	private final String buildDateTime;
	
	public OpenTUNGVersion()
	{
		InputStream is = OpenTUNGVersion.class.getClassLoader().getResourceAsStream("git.properties");
		if(is == null)
		{
			System.out.println("Terminating, cause could not find git.properties file within the jar.\n" +
					"If you are running a jar file, complain to the person who generated/distributed the jar file.\n" +
					"If you are running the project with an IDE, either run the command 'mvn initialize', or add the maven 'initialize' goal to your compile/build routine.");
			System.exit(1);
		}
		JSONObject versionProperties = (JSONObject) JSONParser.parse(new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining()));
		boolean faulty = false;
		
		gitCommitHash = versionProperties.getStringOrNull("git.commit.id");
		if(gitCommitHash == null)
		{
			faulty = true;
			System.out.println("[VersionLoading/ERROR] git.properties does not contain key 'git.commit.id'.");
		}
		
		gitBranch = versionProperties.getStringOrNull("git.branch");
		if(gitBranch == null)
		{
			faulty = true;
			System.out.println("[VersionLoading/ERROR] git.properties does not contain key 'git.branch'.");
		}
		
		buildDateTime = versionProperties.getStringOrNull("git.build.time");
		if(buildDateTime == null)
		{
			faulty = true;
			System.out.println("[VersionLoading/ERROR] git.properties does not contain key 'git.build.time'.");
		}
		
		String gitDirty = versionProperties.getStringOrNull("git.dirty");
		if(gitDirty == null)
		{
			faulty = true;
			System.out.println("[VersionLoading/ERROR] git.properties does not contain key 'git.dirty'.");
		}
		if("true".equals(gitDirty))
		{
			this.gitDirty = true;
		}
		else if("false".equals(gitDirty))
		{
			this.gitDirty = false;
		}
		else
		{
			this.gitDirty = false; //Satisfy compiler.
			System.out.println("[VersionLoading/ERROR] git.properties does not contain key 'git.dirty'.");
		}
		
		if(faulty)
		{
			System.out.println("Terminating, git.properties file is incomplete and does not contain the required version information. Please build the jar properly again, if you obtained it from elsewhere, complain to person in charge.");
			System.exit(1);
		}
	}
	
	public String getGitBranch()
	{
		return gitBranch;
	}
	
	public String getBuildDateTime()
	{
		return buildDateTime;
	}
	
	public String getGitCommitHash()
	{
		return gitCommitHash;
	}
	
	public boolean isGitDirty()
	{
		return gitDirty;
	}
}
