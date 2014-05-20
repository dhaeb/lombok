package lombok.experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method which should be <b>tail call optimized</b>. All
 * recursive calls to your annotated method must be without side effects, which
 * means that the method call to itself must be the last statement of a
 * recursive call of the method. <br/> 
 * For example, consider the following easy factorial method called fac:
 * <p> 
 * <code> 
 * 	int fac(int n){						<br/>
 * 		if(n == 1)						<br/>
 * 			return 1;					<br/>
 * 		 else 							<br/>
 * 			return n * fac(n - 1);		<br/>
 * 	}     								<br/>
 * </code>
 * </p> 
 * This method can not be tail call optimized due to the fact that the
 * recursive call <code>n * fac(n - 1)</code> is not the last statement in the
 * function. Instead of the call there is a multiplication statement, therefore the stack
 * needs to be saved to keep track of the value of the variable n. <br/>
 *  
 * You can rewrite your code with a common pattern of functional programming, 
 * representing such state as parameters of your function:
 * <p>
 * <code>
 * 	int tailrecfac(int n, int ret){		<br/>
 * 		if(n == 1)						<br/>
 * 			return ret;					<br/>
 * 		else 							<br/>
 * 			return fac(n - 1, ret * n); <br/>
 * 	}     								<br/>
 * </code>
 * </p>
 * Considering the new method call it is obvious that the new function
 * <code>tailrecfac</code> calls only itself or returns values of a variable. So
 * in this example you can "forget" the stack because there is no n which needs
 * to be saved.
 * 
 * @author Dan Häberlein
 *
 */
@Target(ElementType.METHOD) 
@Retention(RetentionPolicy.SOURCE) 
public @interface TailCall {}
