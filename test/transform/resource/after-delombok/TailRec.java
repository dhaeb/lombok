import java.math.BigInteger;

public class TailRec {
	
	private static final BigInteger ONE = BigInteger.ONE;
	
	public BigInteger fac(BigInteger n, BigInteger ret) {
		return fac$internal__(n, ret).runRecursiveFunction();
	}
	
	@java.lang.SuppressWarnings("all")
	private final lombok.core.runtimeDependencies.TailCallExpression<BigInteger> fac$internal__(final BigInteger n, final BigInteger ret) {
		if (n.equals(ONE)) {
			return new lombok.core.runtimeDependencies.TailCallExpression.Done<BigInteger>(ret);
		} else {
			return new lombok.core.runtimeDependencies.TailCallExpression.TailCall<BigInteger>(){
				public lombok.core.runtimeDependencies.TailCallExpression<BigInteger> apply() {
					return fac$internal__(n.subtract(ONE), ret.multiply(n));
				}
				
			};
		}
	}
	
}
