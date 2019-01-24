To publish a version to maven repository, 
you should edit your local gradle.properties file.

The file is: `/path-to-user-home/.gradle/gradle.properties`

For example: `~/.gradle/gradle.properties`

Add credentials for nexus repository to `gradle.properties` file.

Example `gradle.properties` file:

```
telcoTeamUsername=********
telcoTeamPassword=************************
```

Then execute `gradle` `publish` task on the project.

For example, to publish the project `lock-provider`, 
you need to execute the following command in project root:

```
gradle :lock-provider:publish
``` 

The repository will not allow you to publish the same version twice.
You need to change version of the artifact every time you want to publish.

You can change version in `build.gradle` file of the sub-project.

```
build.gradle > publishing > publications > mavenJava > version
```

Please change the version wisely.