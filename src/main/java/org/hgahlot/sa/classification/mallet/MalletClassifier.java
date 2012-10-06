/**
 * 
 */
package org.hgahlot.sa.classification.mallet;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.util.Randoms;

/**
 * @author
 *
 */
public class MalletClassifier {

	/**
	 * Train a MaxEnt classifier using a list of training instances 
	 * (for information on creating instance lists, see the data import 
	 * developer's guide)
	 * 
	 * @param trainingInstances
	 * @return
	 */
	public Classifier trainClassifier(InstanceList trainingInstances) {

		// Here we use a maximum entropy (ie polytomous logistic regression)                               
		//  classifier. Mallet includes a wide variety of classification                                   
		//  algorithms, see the JavaDoc API for details.                                                   

		ClassifierTrainer trainer = new MaxEntTrainer();
		return trainer.train(trainingInstances);
	}

	/**
	 * Restore a saved classifier
	 * 
	 * @param serializedFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Classifier loadClassifier(File serializedFile)
	throws FileNotFoundException, IOException, ClassNotFoundException {

		// The standard way to save classifiers and Mallet data                                            
		//  for repeated use is through Java serialization.                                                
		// Here we load a serialized classifier from a file.                                               

		Classifier classifier;

		ObjectInputStream ois =
			new ObjectInputStream (new FileInputStream (serializedFile));
		classifier = (Classifier) ois.readObject();
		ois.close();

		return classifier;
	}


	public void saveClassifier(Classifier classifier, File serializedFile)
	throws IOException {

		// The standard method for saving classifiers in                                                   
		//  Mallet is through Java serialization. Here we                                                  
		//  write the classifier object to the specified file.                                             

		ObjectOutputStream oos =
			new ObjectOutputStream(new FileOutputStream (serializedFile));
		oos.writeObject (classifier);
		oos.close();
	}

	/**
	 * Use a trained classifier to guess the class of new data. 
	 * We first read in raw instance data from a file, pass the data through the same pipe 
	 * that was used to load the original training data, then finally pass the instances 
	 * through the classifier and print the classification scores.
	 * Note that in this example, we're reading the instances one by one, without saving 
	 * them anywhere. This stream-based approach saves memory, but it may also be 
	 * appropriate to keep the instances around.
	 * 
	 * @param classifier
	 * @param file
	 * @throws IOException
	 */
	public void printLabelings(Classifier classifier, File fileOrDir, File outFile, boolean isDir) throws IOException {
		Iterator instances;
		if(!isDir){
			// Create a new iterator that will read raw instance data from                                     
			//  the lines of a file.                                                                           
			// Lines should be formatted as:                                                                   
			//                                                                                                 
			//   [name] [label] [data ... ]                                                                    
			//                                                                                                 
			//  in this case, "label" is ignored.                                                              

			CsvIterator csvReader =
				new CsvIterator(new FileReader(fileOrDir),
						"(\\w+)\\s+(\\w+)\\s+(.*)",
						3, 2, 1);  // (data, label, name) field indices
			// Create an iterator that will pass each instance through                                         
			//  the same pipe that was used to create the training data                                        
			//  for the classifier.
			instances = classifier.getInstancePipe().newIteratorFrom(csvReader);
		} else {
			FileIterator fileReader = new FileIterator(fileOrDir,
						new TxtFilter(),
						FileIterator.LAST_DIRECTORY);
			// Create an iterator that will pass each instance through                                         
			//  the same pipe that was used to create the training data                                        
			//  for the classifier.
			instances = classifier.getInstancePipe().newIteratorFrom(fileReader);
		}

		//open a writer stream to write the output to
		PrintWriter pw = new PrintWriter(outFile);

		// Classifier.classify() returns a Classification object                                           
		//  that includes the instance, the classifier, and the                                            
		//  classification results (the labeling). Here we only                                            
		//  care about the Labeling.                                                                       
		while (instances.hasNext()) {
			Instance inst = (Instance) instances.next();
			Labeling labeling = classifier.classify(inst).getLabeling();

			// print the labels with their weights in descending order (ie best first)                     
			pw.println(inst.getName());
			for (int rank = 0; rank < labeling.numLocations(); rank++){
				//				System.out.print(labeling.getLabelAtRank(rank) + ":" +
				//						labeling.getValueAtRank(rank) + " ");
				pw.print(labeling.getLabelAtRank(rank) + ":" +
						labeling.getValueAtRank(rank) + " ");
			}
			pw.println();
			System.out.println();

		}
		pw.close();
	}
	
	/**
	 * Predict the label for a single instance.
	 * @param classifier
	 * @param instance
	 * @return Labeling object for the instance
	 */
	public Labeling predictLabel(Classifier classifier, Instance instance) {
		Instance pipedInst = classifier.getInstancePipe().instanceFrom(instance);
		Labeling labeling = classifier.classify(pipedInst).getLabeling();
		return labeling;
	}

	/**
	 * In order to know whether a classifier is producing reliable predictions, we can 
	 * test it by providing additional labeled data and comparing the predicted labels 
	 * to the actual labels. This method reads in testing instances from a file 
	 * and reports several evaluation metrics, including accuracy, precision, recall, 
	 * and F-measure.
	 * 
	 * @param classifier
	 * @param file
	 * @throws IOException
	 */
	public void evaluate(Classifier classifier, File file) throws IOException {

		// Create an InstanceList that will contain the test data.                                         
		// In order to ensure compatibility, process instances                                             
		//  with the pipe used to process the original training                                            
		//  instances.                                                                                     

		InstanceList testInstances = new InstanceList(classifier.getInstancePipe());

		// Create a new iterator that will read raw instance data from                                     
		//  the lines of a file.                                                                           
		// Lines should be formatted as:                                                                   
		//                                                                                                 
		//   [name] [label] [data ... ]                                                                    

		CsvIterator reader =
			new CsvIterator(new FileReader(file),
					"(\\w+)\\s+(\\w+)\\s+(.*)",
					3, 2, 1);  // (data, label, name) field indices               

		// Add all instances loaded by the iterator to                                                     
		//  our instance list, passing the raw input data                                                  
		//  through the classifier's original input pipe.                                                  

		testInstances.addThruPipe(reader);

		Trial trial = new Trial(classifier, testInstances);

		// The Trial class implements many standard evaluation                                             
		//  metrics. See the JavaDoc API for more details.                                                 

		System.out.println("Accuracy: " + trial.getAccuracy());

		// precision, recall, and F1 are calcuated for a specific                                          
		//  class, which can be identified by an object (usually                                           
		//  a String) or the integer ID of the class                                                       

		System.out.println("F1 for class 'good': " + trial.getF1("good"));

		System.out.println("Precision for class '" +
				classifier.getLabelAlphabet().lookupLabel(1) + "': " +
				trial.getPrecision(1));
	}


	/**
	 * Produce several random splits of the data into testing and training sets 
	 * and return a Trial object that can be used to report evaluation metrics.
	 * 
	 * @param instances
	 * @return
	 */
	public Trial testTrainSplit(InstanceList instances, double trainingSplit, 
			double testingSplit, double validationSplit) {

		int TRAINING = 0;
		int TESTING = 1;
		int VALIDATION = 2;

		// Split the input list into training (90%) and testing (10%) lists.                               
		// The division takes place by creating a copy of the list,                                        
		//  randomly shuffling the copy, and then allocating                                               
		//  instances to each sub-list based on the provided proportions.                                  

		InstanceList[] instanceLists =
			instances.split(new Randoms(),
					new double[] {trainingSplit, testingSplit, 
				validationSplit});

		// The third position is for the "validation" set,                                                 
		//  which is a set of instances not used directly                                                  
		//  for training, but available for determining                                                    
		//  when to stop training and for estimating optimal                                               
		//  settings of nuisance parameters.                                                               
		// Most Mallet ClassifierTrainers can not currently take advantage                                 
		//  of validation sets.                                                                            

		Classifier classifier = trainClassifier( instanceLists[TRAINING] );
		return new Trial(classifier, instanceLists[TESTING]);
	}


	/** This class illustrates how to build a simple file filter */
	class TxtFilter implements FileFilter {

		/** Test whether the string representation of the file 
		 *   ends with the correct extension. Note that {@ref FileIterator}
		 *   will only call this filter if the file is not a directory,
		 *   so we do not need to test that it is a file.
		 */
		public boolean accept(File file) {
			return file.toString().endsWith(".txt");
		}
	}
}
