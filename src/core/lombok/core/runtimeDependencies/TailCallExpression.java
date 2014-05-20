package lombok.core.runtimeDependencies;

import java.util.Arrays;
import java.util.List;

import org.mangosdk.spi.ProviderFor;

@ProviderFor(RuntimeDependencyInfo.class)
public abstract class TailCallExpression<T> implements RuntimeDependencyInfo {

	@Override 
	public List<String> getRuntimeDependencies() {
		return Arrays.asList(
				new String[]{
						TailCallExpression.class.getResource(TailCallExpression.class.getName()).getFile()
						}
				);
	}
	
	@Override 
	public List<String> getRuntimeDependentsDescriptions() {
		return Arrays.asList(
				new String[]{
						String.format("@%s When using this kind of optimization, you need to add lombok-runtime.jar to your classpath. This is why the optimization uses a runtime mechanism based on objects.", lombok.experimental.TailCall.class.getName())				
							}
				);
	}
	
	public abstract TailCallExpression<T> apply();

	public abstract T getResult();

	public T runRecursiveFunction() {
		boolean isFinished = false;
		TailCallExpression<T> expression = this;
		do {
			if (Done.class.isInstance(expression)) {
				isFinished = true;
			} else {
				expression = expression.apply();
			}
		} while (!isFinished);
		return expression.getResult();
	}

	public abstract static class TailCall<T> extends TailCallExpression<T> {

		public abstract TailCallExpression<T> apply();

		@Override
		public T getResult() {
			throw new UnsupportedOperationException("The result is not computed yet! You need to call runRecursiveFunction() first!");
		}

	}

	public static <T> Done<T> createResult(T result){
		return new Done<T>(result);
	}
	
	public static class Done<T> extends TailCallExpression<T> {

		private T result;

		public Done(T result) {
			this.result = result;
		}

		@Override
		public TailCallExpression<T> apply() {
			throw new UnsupportedOperationException("Done instance can't be applied to anything, the result is retrievable using the result method.");
		}

		@Override
		public T getResult() {
			return result;
		}

	}

}
