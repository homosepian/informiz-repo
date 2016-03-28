![alt tag](http://informiz.org/wp-content/uploads/2015/08/signs-banner.jpg)

# Informiz Repository
This is the repository application for [Informiz.org](http://informiz.org/), built upon [Neo4j](http://neo4j.org/).

## Information Made Accessible
[Informiz.org](http://informiz.org/) is a repository of short explanations about things. You may think of it as a graphic TL;DR version of Wikipedia, which can be searched directly or embedded in other websites via a [plugin](https://wordpress.org/plugins/informiz/). The information-snippets are dubbed *informiz*. 
![alt text](http://informiz.org/wp-content/uploads/2015/10/nano.png)
You can see the plugin in action on the [informiz.org](http://informiz.org/informiz-on-demand/) site.

## Functionality
This repository is currently comprised of a Noe4j graph database, an Elasticsearch cluster and a Flume agent streaming events between the two.
The application exposes a [Spark-Java](http://www.sparkjava.com/) REST API for retrieving a landscape-graph of connections between informiz.
![alt text](http://informiz.org/wp-content/uploads/2015/10/graph.png)

## Building the project
Run mvn clean install -DskipITs to have the jar files built and copied to the docker build directories.

Due to a compatability issue between Flume and Elasticsearch, you will need to manually build [ElasticsearchSink2](https://github.com/lucidfrontier45/ElasticsearchSink2) and place the jar under docker/flume/build.

## Installation
### With shell scripts
1. Run the deploy.sh script in the docker/ directory. It will deploy all the components and load sample data to Neo4j and Elasticsearch.
* Your REST service will be available at e.g http://localhost:8080/graph?informi=869
2. To remove the application, run the cleanup.sh script under the same directory.
### With docker-compose
1. Run "docker-compose up" under the docker/ directory. It will deploy all the components and load sample data to Neo4j and Elasticsearch.
* Your REST service will be available at e.g http://localhost:8080/graph?informi=869
2. Note that after stopping the application you have to manually execute "rm -r $HOME/neo4j/data" in order to delete the smaple data.


