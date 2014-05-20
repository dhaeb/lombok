import lombok.experimental.TailCall;

public class TailRecSwitch {
	
	@TailCall 
	public int fac(int n, int ret) {
		switch(n){
			case 1 : return ret;
			default : return fac(n - 1, ret * n);
		}
	}
	
}
