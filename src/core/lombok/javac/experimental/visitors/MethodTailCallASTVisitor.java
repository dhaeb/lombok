package lombok.javac.experimental.visitors;

import static lombok.javac.handlers.JavacHandlerUtil.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.core.runtimeDependencies.TailCallExpression.Done;
import lombok.core.runtimeDependencies.TailCallExpression.TailCall;
import lombok.javac.JavacASTAdapter;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

public class MethodTailCallASTVisitor extends JavacASTAdapter {
	
	private static final class StateToPos {
		private int pos;
		private int label;
		
		public StateToPos(int pos, int label) {
			this.pos = pos;
			this.label = label;
		}

		@Override public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + label;
			result = prime * result + pos;
			return result;
		}

		@Override public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			StateToPos other = (StateToPos) obj;
			if (label != other.label) return false;
			if (pos != other.pos) return false;
			return true;
		}

	}

	private static interface ReturnNodeHandler<T extends JCTree> {
		void replaceReturnFor(T node, JCTree returnDecl, JCStatement newReturn);
	}
	
	private static class JCBlockReturnNodeHandler implements ReturnNodeHandler<JCBlock> {
		
		@Override public void replaceReturnFor(JCBlock node, JCTree returnDecl, JCStatement newReturn) {
			ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
			for (int i = 0; i < node.stats.size() - 1; i++) {
				statements.append(node.stats.get(i));
			}
			statements.append(newReturn);
			node.stats = statements.toList();
		}
		
	}
	
	private static class IfReturnNodeHandler implements ReturnNodeHandler<JCIf> {
		
		@Override public void replaceReturnFor(JCIf node, JCTree returnDecl, JCStatement newReturn) {
			if(node.thenpart == returnDecl){
				System.err.println("if");
				node.thenpart = newReturn;
			} else if(node.elsepart == returnDecl) {
				node.elsepart = newReturn;
			} else {
				System.err.println("fail!");
				throw new RuntimeException("The returnnode is not statement of the if else clause.");
			}
		}
		
	}
	
	private static class CaseReturnNodeHandler implements ReturnNodeHandler<JCCase> {
		
		@Override public void replaceReturnFor(JCCase node, JCTree returnDecl, JCStatement newReturn) {
			assert node.stats.size() == 1;
			node.stats = List.of(newReturn);
		}
		
	}
	
	
	
	private static Map<Class<? extends JCTree>, ReturnNodeHandler<?>> TYPE_TO_HANDLER_MAP = new HashMap<Class<? extends JCTree>, ReturnNodeHandler<?>>();
	
	static {
		TYPE_TO_HANDLER_MAP.put(JCBlock.class, new JCBlockReturnNodeHandler());
		TYPE_TO_HANDLER_MAP.put(JCIf.class, new IfReturnNodeHandler());
		TYPE_TO_HANDLER_MAP.put(JCCase.class, new CaseReturnNodeHandler());
	}
	
	private static final String PENDING = "Pending";
	
	private static final int RETURN_FOUND = 23;
	
	private Set<StateToPos> openStmts = new HashSet<StateToPos>();
	
	private boolean isInReturnStmt;
	private boolean beforeFirstStmtInReturn;
	
	private JCExpression returntype;
	private JCExpression newReturnType;

	private JCMethodDecl methodDecl;

	private JCMethodDecl redef;

	private JavacNode newMethod;
	private JavacNode oldMethod;
	
	public MethodTailCallASTVisitor(JavacNode oldMethod, JavacNode newMethod, JCExpression returnType) {
		this.oldMethod = oldMethod;
		this.newMethod = newMethod;
		this.methodDecl = (JCMethodDecl) oldMethod.get();
		this.redef = (JCMethodDecl) newMethod.get();
		this.returntype = returnType;
		this.newReturnType = redef.restype;
	}
	
	@Override public void visitType(JavacNode typeNode, JCClassDecl type) {
		System.out.println("visitType");
		System.out.println(typeNode.getName());
	}
	
	@Override public void visitStatement(JavacNode statementNode, JCTree statement) {
		System.out.println("---------visitStatement------------");
		System.out.println(statement);
		System.out.println("pos " + statement.pos);
		System.out.println("kind " + statement.getKind());
		System.out.println("-----------------------------------");
		if (isInReturnStmt) {
			if (beforeFirstStmtInReturn) {
				beforeFirstStmtInReturn = false;
				JavacNode returnNode = statementNode.directUp();
				JavacNode enclosingNode = returnNode.directUp();
				JCTree enclosingExpression = enclosingNode.get();
				JavacTreeMaker treeMaker = enclosingNode.getTreeMaker();
				JCExpression returnable = null;
				if (Kind.METHOD_INVOCATION.equals(statement.getKind())) {
					JavacNode typeNode = oldMethod.up();
					treeMaker = typeNode.getTreeMaker();
					System.out.println("method invocation");
					
					JCMethodInvocation currentInvocation = (JCMethodInvocation)statement;

					// return func_$internal__(args_from_func)
					JCStatement stmt = treeMaker.Return(treeMaker.Apply(null, treeMaker.Ident(redef.name), currentInvocation.args));
					
					// public apply() {...}
					JCMethodDecl def = treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), 
														   enclosingNode.toName("apply"), 
														   newReturnType, 
														   List.<JCTypeParameter>nil(), 
														   List.<JCVariableDecl>nil(), 
														   List.<JCExpression>nil(), 
														   treeMaker.Block(0, List.<JCStatement>of(stmt)), 
														   null);
					// new da.kdi.tailcalls.TailCallExpression()  {...}
					JCClassDecl anonymousClassDef = treeMaker.AnonymousClassDef(treeMaker.Modifiers(0), List.<JCTree>of(def));
					
					
					System.out.println("typeDecl");
					JCClassDecl typeDecl = (JCClassDecl)typeNode.get();
					
					// new da.kdi.tailcalls.TailCallExpression()  {...}
					returnable = createNewInstance(List.<JCExpression>nil(), 
												  treeMaker,
												  createIndetifierForNestedClass(TailCall.class, statementNode), 
												  anonymousClassDef,
												  null);
				} else {
					System.out.println(enclosingExpression);
					returnable = createNewInstance(List.of((JCExpression) statement), 
												   treeMaker, 
												   createIndetifierForNestedClass(Done.class, statementNode), 
												   null,
												   null);
				}
				returnable = recursiveSetGeneratedBy(returnable, statementNode.up().get(), statementNode.getContext());
				
				ReturnNodeHandler handler = TYPE_TO_HANDLER_MAP.get(enclosingExpression.getClass());
				if (handler == null) {
					throw new RuntimeException("handler was not found for type " + enclosingExpression.getClass());
				} else {
					handler.replaceReturnFor(enclosingExpression, returnNode.get(), treeMaker.Return(returnable));
				}
			}
		}
		Kind kind = statement.getKind();
		if (Kind.RETURN.equals(kind)) {
			System.out.println("set to true");
			isInReturnStmt = true;
			beforeFirstStmtInReturn = true;
			openStmts.add(new StateToPos(statement.pos, PENDING.hashCode()));
			openStmts.add(new StateToPos(statement.pos, RETURN_FOUND));
		}
	}
	
	private JCNewClass createNewInstance(List<JCExpression> args, JavacTreeMaker treeMaker, JCExpression nameOfClass, JCClassDecl typedef, JCExpression encl) {
		JCTypeApply generizedType = treeMaker.TypeApply(nameOfClass, List.of(returntype));
		return treeMaker.NewClass(encl, null, generizedType, args, typedef);
	}
	
	@Override public void endVisitStatement(JavacNode statementNode, JCTree statement) {
		System.out.println("---------endVisitStatement------------");
		System.out.println(statement);
		System.out.println("pos " + statement.pos);
		System.out.println("kind " + statement.getKind());
		System.out.println("-----------------------------------");
		StateToPos returnStmt = new StateToPos(statement.pos, RETURN_FOUND);
		StateToPos pendingStmt = new StateToPos(statement.pos, PENDING.hashCode());
		if (openStmts.contains(returnStmt)) {
			openStmts.remove(returnStmt);
			isInReturnStmt = false;
			System.out.println("set to false");
		} else if (openStmts.contains(pendingStmt)) {
			beforeFirstStmtInReturn = false;
			openStmts.remove(pendingStmt);
		}
		
	}
	
}