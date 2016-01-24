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
1. You need [Neo4j](http://neo4j.com/download) and RabbitMQ running on localhost to run this application.
2. Start your local Neo4j Server. Import the data using the commands in the file [data.sql](/src/test/resources/data.sql).
3. Change the Neo4j credentials in the Dockerfile under docker/lands-rest/ to your own.
4. Use the scripts under docker/lands-rest/ and docker/lands-ser/ to build and run the service and endpoint.
* Your REST serive will be available at http://localhost:8080/graph?informi=xyz
5. Use the stop.sh scripts in the same directories to stop the service and endpoint.


