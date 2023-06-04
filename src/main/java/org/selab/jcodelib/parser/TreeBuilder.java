package jcodelib.parser;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import file.FileIOManager;
import org.selab.jcodelib.parser.JavaCodeVisitor;
import org.selab.jcodelib.parser.TreeNode;

public class TreeBuilder {

	public static TreeNode buildTreeFromFile(File f) throws IOException {
		TreeNode tree = buildTreeFromSource(FileIOManager.getContent(f));

		return tree;
	}

	public static TreeNode buildTreeFromSource(String source) throws IOException {
		TreeNode root = new TreeNode();
		CompilationUnit cu = getCompilationUnit(source);
		JavaCodeVisitor visitor = new JavaCodeVisitor(root);
		cu.accept(visitor);

		return root;
	}

	public static CompilationUnit getCompilationUnit(String source) {
		ASTParser parser = ASTParser.newParser(AST.JLS10);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		CompilationUnit cu = (CompilationUnit)parser.createAST(null);

		return cu;
	}

}
