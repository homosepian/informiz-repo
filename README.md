![alt tag](http://informiz.org/wp-content/uploads/2015/08/signs-banner.jpg)

# Informiz Repository
This is the repository application for [Informiz.org](http://informiz.org/), built upon [Neo4j](http://neo4j.org/).

## Information Made Accessible
[Informiz.org](http://informiz.org/) is a repository of short explanations about things. You may think of it as a graphic TL;DR version of Wikipedia, which can be searched directly or 
embedded in other websites via a [plugin](https://wordpress.org/plugins/informiz/). The information-snippets are dubbed *informiz*. 
![alt text](http://informiz.org/wp-content/uploads/2015/10/nano.png)
You can see the plugin in action on the [informiz.org](http://informiz.org/informiz-on-demand/) site.

## Functionality
Get a landscape-graph of connections between informiz. REST API exposed with [Spark-Java](http://www.sparkjava.com/).
![alt text](http://informiz.org/wp-content/uploads/2015/10/graph.png)

## Installation
Start your local Neo4j Server [(Download & Install)](http://neo4j.com/download). Import the data using the commands in the file [data.sql](/src/test/resources/data.sql).

Build the application with:
```bash
mvn package
```
Run the application with:
```bash
java -jar lands-service-1.0-SNAPSHOT.jar "neo4j_user" "neo4j_password" "localhost" &
/path/to/java8/bin/java -jar lands-endpoint-1.0-SNAPSHOT.jar "localhost" &
```
Where neo4j_user and neo4j_password are your Neo4j credentials (application will fail without credentials).
Your REST serive will be available at http://localhost:8080/graph?informi=xyz


