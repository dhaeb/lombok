public class TailRecSwitch {
	
	public int fac(int n, int ret) {
		return fac$internal__(n, ret).runRecursiveFunction();
	}

	@java.lang.SuppressWarnings("all")
	private final de.kdi.tailcalls.TailCallExpression<Integer> fac$internal__(final int n, final int ret) {
		switch (n) {
		case 1: 
			return new de.kdi.tailcalls.TailCallExpression.Done<Integer>(ret);
		default: 
			return new de.kdi.tailcalls.TailCallExpression.TailCall<Integer>(){
				public de.kdi.tailcalls.TailCallExpression<Integer> apply() {
					return fac$internal__(n - 1, ret * n);
				}
			
			};
		}
	}
	
}