package org.selab.jcodelib.element;

import java.util.ArrayList;
import java.util.List;

import at.aau.softwaredynamics.classifier.entities.SourceCodeChange;
import org.selab.jcodelib.util.CodeUtils;

public class IJMChange {

	private String changeType;
	private String entityType;
	private int oldStartPos;
	private int oldLength;
	private int newStartPos;
	private int newLength;
	private IJMChange parent;
	private List<IJMChange> children;

	public IJMChange(String changeType, String entityType, int oldStartPos, int oldLength, int newStartPos,
			int newLength) {
		super();
		this.changeType = changeType;
		this.entityType = entityType;
		this.oldStartPos = oldStartPos;
		this.oldLength = oldLength;
		this.newStartPos = newStartPos;
		this.newLength = newLength;
		this.parent = null;
		this.children = new ArrayList<>();
	}

	public IJMChange(SourceCodeChange change) {
		super();
		this.changeType = change.getAction().getName();
		this.entityType = CodeUtils.getTypeName(change.getNodeType());
		this.oldStartPos = change.getSrcInfo().getPosition();
		this.oldLength = change.getSrcInfo().getLength();
		this.newStartPos = change.getDstInfo().getPosition();
		this.newLength = change.getDstInfo().getLength();
		this.parent = null;
		this.children = new ArrayList<>();
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public int getOldStartPos() {
		return oldStartPos;
	}

	public void setOldStartPos(int oldStartPos) {
		this.oldStartPos = oldStartPos;
	}

	public int getOldLength() {
		return oldLength;
	}

	public void setOldLength(int oldLength) {
		this.oldLength = oldLength;
	}

	public int getNewStartPos() {
		return newStartPos;
	}

	public void setNewStartPos(int newStartPos) {
		this.newStartPos = newStartPos;
	}

	public int getNewLength() {
		return newLength;
	}

	public void setNewLength(int newLength) {
		this.newLength = newLength;
	}

	public IJMChange getParent() {
		return parent;
	}

	public void setParent(IJMChange parent) {
		this.parent = parent;
	}

	public void addChild(IJMChange c) {
		c.setParent(this);
		this.children.add(c);
	}

	public List<IJMChange> getChildren() {
		return children;
	}
}
