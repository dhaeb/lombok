public class TailRecSwitch {
	
	public int fac(int n, int ret) {
		return fac$internal__(n, ret).runRecursiveFunction();
	}

	@java.lang.SuppressWarnings("all")
	private final lombok.core.runtimeDependencies.TailCallExpression<Integer> fac$internal__(final int n, final int ret) {
		switch (n) {
		case 1: 
			return new lombok.core.runtimeDependencies.TailCallExpression.Done<Integer>(ret);
		default: 
			return new lombok.core.runtimeDependencies.TailCallExpression.TailCall<Integer>(){
				public lombok.core.runtimeDependencies.TailCallExpression<Integer> apply() {
					return fac$internal__(n - 1, ret * n);
				}
			
			};
		}
	}
	
}