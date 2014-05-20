package lombok.javac.handlers;

import static lombok.javac.handlers.JavacHandlerUtil.*;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.core.runtimeDependencies.TailCallExpression;
import lombok.experimental.TailCall;
import lombok.javac.Javac;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.ResolutionResetNeeded;
import lombok.javac.TreeMirrorMaker;
import lombok.javac.experimental.visitors.MethodTailCallASTVisitor;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

@ProviderFor(JavacAnnotationHandler.class)
@ResolutionResetNeeded
@HandlerPriority(66000) // run after val!
public class HandleTailCall extends JavacAnnotationHandler<TailCall>{
		
	@Override 
	public void handle(AnnotationValues<TailCall> annotation, JCAnnotation ast, JavacNode annotationNode) {
		deleteAnnotationIfNeccessary(annotationNode, TailCall.class);
		deleteImportFromCompilationUnit(annotationNode, "lombok.TailCall");
		optimize(annotationNode);
	}

	private void optimize(JavacNode annotationNode) {
		JavacNode methodNode = annotationNode.up();
		if(methodNode.getKind() != lombok.core.AST.Kind.METHOD){methodNode.addWarning("You can only optimize methods using TailCall!"); return;}
		JCMethodDecl methodDecl = (JCMethodDecl) methodNode.get();
		if(methodDecl == null) return;
		
		JavacTreeMaker treeMaker = methodNode.getTreeMaker();
		String methodName = methodDecl.name.toString();
		
		JCModifiers newFlags = (JCModifiers) methodDecl.mods.clone();
		long flags = methodDecl.mods.flags;
		newFlags.flags = (flags & Flags.STATIC) | Flags.PRIVATE | Flags.FINAL; 
		JavacNode typeNode = methodNode.up();
		
		
		JCExpression returnType = (JCExpression)methodDecl.restype.clone();
		if(Javac.isPrimitive(returnType)){
			String box = box(returnType.toString());
			returnType = treeMaker.Ident(typeNode.toName(box));
		}
		
		JCExpression newReturnType = treeMaker.TypeApply(createIndetifierForNestedClass(TailCallExpression.class, methodNode), 
									     List.<JCExpression>of(returnType));
		
		String internalMethodName = createInternalRepresentationOfMethodName(methodName);
		TreeMirrorMaker treeCopier = new TreeMirrorMaker(treeMaker);
		
		JCMethodDecl redef = recursiveSetGeneratedBy(treeMaker.MethodDef(newFlags,
				toInternal(methodNode, internalMethodName), 
				newReturnType, 
				methodDecl.typarams, 
				cloneAndToFinal(methodDecl.params), 
				methodDecl.thrown, 
				treeCopier.copy(methodDecl.body),
				methodDecl.defaultValue), annotationNode.get(), annotationNode.getContext());

		
		JavacNode newMethodNode = injectMethod(typeNode, redef);
		newMethodNode.traverse(new MethodTailCallASTVisitor(methodNode, newMethodNode, returnType));
		
		methodDecl.body = recursiveSetGeneratedBy(delegateToInternalTailCallOptimizedMethod(methodDecl, typeNode, internalMethodName), annotationNode.get(), annotationNode.getContext());
		newMethodNode.getAst().setChanged();
	}

	private List<JCVariableDecl> cloneAndToFinal(List<JCVariableDecl> params) {
		ListBuffer<JCVariableDecl> decls = new ListBuffer<JCVariableDecl>();
		for(JCVariableDecl decl : params){
			JCVariableDecl paramClone = (JCVariableDecl) decl.clone();
			JCModifiers modifiersClone = (JCModifiers)paramClone.mods.clone();
			modifiersClone.flags |= Flags.FINAL;
			paramClone.mods = modifiersClone;
			decls.append(paramClone);
		}
		return decls.toList();
	}

	private JCBlock delegateToInternalTailCallOptimizedMethod(JCMethodDecl methodDecl, JavacNode typeNode, String internalMethodName) {
		JavacTreeMaker treeMaker = typeNode.getTreeMaker();
		ListBuffer<JCExpression> argsBuffer = new ListBuffer<JCExpression>(); 
		for(JCVariableDecl paramDecl : methodDecl.params){
			argsBuffer.append(treeMaker.Ident(paramDecl.name));
		}
		JCMethodInvocation internalInvocation = treeMaker.Apply(List.<JCExpression>nil(), 
														    treeMaker.Ident(typeNode.toName(internalMethodName)), 
														    argsBuffer.toList());
		JCMethodInvocation select = treeMaker.Apply(List.<JCExpression>nil(), treeMaker.Select(internalInvocation, typeNode.toName("runRecursiveFunction")), List.<JCExpression>nil());
		JCStatement stmt = treeMaker.Return(select);
		List<JCStatement> statements = List.of(stmt);
		return treeMaker.Block(0, statements);
	}
	
	private Name toInternal(JavacNode methodNode, String internalRepresentation) {
		return methodNode.toName(internalRepresentation);
	}

	private String createInternalRepresentationOfMethodName(String methodName) {
		return String.format("%s$internal__", methodName);
	}
	
}
 