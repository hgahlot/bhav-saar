bhav-saar, Hindi for 'summary of emotions', is a concept-level sentiment analysis engine. Given an entity 
or a list of entities as input it is able to figure out the sentiment expressed towards these entities 
in the given set of documents/file/text. Please read the config/sa.config file for information about the
input parameters. Note that you will need to include the stanford-parser library provided with this 
project along with the ones downloaded through Maven.

The start point is the org.hgahlot.sa.manager.SentimentAnalyzerMain class. You need to provide the 
config/sa.config as an argument to this class and set the properties in this file. If you want to use 
coreference resolution then you need to mark sa.coref.resolve=true and need to install the BART 
coref system (http://www.bart-coref.org/) and start the web service. You can then provide the url of the 
BART server in sa.coref.bart.url.