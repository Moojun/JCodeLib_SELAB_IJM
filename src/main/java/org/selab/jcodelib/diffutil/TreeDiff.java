package org.selab.jcodelib.diffutil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.gumtreediff.gen.TreeGenerator;
//import com.github.gumtreediff.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Matcher;

import at.aau.softwaredynamics.classifier.AbstractJavaChangeClassifier;
import at.aau.softwaredynamics.classifier.JChangeClassifier;
import at.aau.softwaredynamics.classifier.NonClassifyingClassifier;
import at.aau.softwaredynamics.classifier.entities.FileChangeSummary;
import at.aau.softwaredynamics.gen.DocIgnoringTreeGenerator;
import at.aau.softwaredynamics.gen.OptimizedJdtTreeGenerator;
import at.aau.softwaredynamics.gen.SpoonTreeGenerator;
import at.aau.softwaredynamics.matchers.JavaMatchers;
import at.aau.softwaredynamics.runner.util.ClassifierFactory;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.Move;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
//import edu.fdu.se.cldiff.CLDiffLocal;
import file.FileIOManager;
import org.selab.jcodelib.element.CDChange;
import org.selab.jcodelib.element.IJMChange;
import org.selab.jcodelib.util.CodeUtils;
import kr.ac.seoultech.selab.esscore.model.Script;
import kr.ac.seoultech.selab.esscore.util.LASScriptConverter;


public class TreeDiff {


	public static DiffResult diffIJM(File srcFile, File dstFile) {
		//Default options from the example: -c None -m IJM -w FS -g OTG
		return diffIJM(srcFile, dstFile, "None", "IJM", "OTG");
	}

	public static DiffResult diffIJM(File srcFile, File dstFile, String optClassifier, String optMatcher, String optGenerator) {
		Class<? extends AbstractJavaChangeClassifier> classifierType = getClassifierType(optClassifier);
		Class<? extends Matcher> matcher = getMatcherTypes(optMatcher);
		TreeGenerator generator = getTreeGenerator(optGenerator);

		ClassifierFactory factory = new ClassifierFactory(classifierType, matcher, generator);
		FileChangeSummary summary = new FileChangeSummary("", "", srcFile.getName(), dstFile.getName());

		try {
			String oldCode = FileIOManager.getContent(srcFile);
			String newCode = FileIOManager.getContent(dstFile);

			long startTime = System.currentTimeMillis();
			AbstractJavaChangeClassifier classifier = factory.createClassifier();
			try {
				classifier.classify(oldCode, newCode);
				summary.setChanges(classifier.getCodeChanges());
				summary.setMetrics(classifier.getMetrics());
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> changes = classifier.getCodeChanges();

			return new DiffResult(getIJMChanges(changes), endTime-startTime);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static List<IJMChange> getIJMChanges(List<at.aau.softwaredynamics.classifier.entities.SourceCodeChange> changes) {
		//Group changes.
		List<IJMChange> grouped = new ArrayList<>();
		for(at.aau.softwaredynamics.classifier.entities.SourceCodeChange change : changes) {
			//No parent changes indicates this is the change subtree root.
			if(change.getParentChanges().size() == 0) {
				grouped.add(convert(change));
			}
		}
		return grouped;
	}

	public static IJMChange convert(at.aau.softwaredynamics.classifier.entities.SourceCodeChange change) {
		IJMChange converted = new IJMChange(change);
		for(at.aau.softwaredynamics.classifier.entities.SourceCodeChange child : change.getChildrenChanges()) {
			converted.addChild(convert(child));
		}
		return converted;
	}

	private static Class<? extends Matcher> getMatcherTypes(String option) {
		switch(option) {
		case "GT":
			return CompositeMatchers.ClassicGumtree.class;
		case "IJM":
			return JavaMatchers.IterativeJavaMatcher_V2.class;
		case "IJM_Spoon":
			return JavaMatchers.IterativeJavaMatcher_Spoon.class;
		}

		return null;
	}

	private static Class<? extends AbstractJavaChangeClassifier> getClassifierType(String option) {
		switch (option) {
		case "Java": return JChangeClassifier.class;
		case "None": return NonClassifyingClassifier.class;
		default: return JChangeClassifier.class;
		}
	}

	private static TreeGenerator getTreeGenerator(String option) {
		switch (option)
		{
		case "OTG": return new OptimizedJdtTreeGenerator();
		/*case "JTG": return new JdtTreeGenerator();*/
		case "JTG1": return new DocIgnoringTreeGenerator();
		case "SPOON":
			return new SpoonTreeGenerator();
		}
		return null;
	}
}
