ElasticAuth-Jetty
=================

Elastic Search Authentication &amp; Authorization Module based on Jetty. 
This project is intended to solve Authentication & Authorization problem of Front End GUIs run on Elastic Search, such as Kibana.


  I have used Jetty as HTTP Transport module of Jetty based on work of https://github.com/sonian/elasticsearch-jetty. 
Currently I am aiming for Corporate Environments which uses LDAP based Authentication Modules.

  Rationale Behind Project:  We are serving various text based data(such as logs) to our fellow collegues in our Corporate Environment. With successfully using Elastic Search and Kibana in Front, yet some of our data might be sensitive (System logs, or Application Logs contain Customer related information). So we need Authentication&Authorization based on Project Teams and System Admins. So far we create different indexes based on Authorization needs and filter by User Credentials fetched via LDAP or any JettyLoginModule.  
