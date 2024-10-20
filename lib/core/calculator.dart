import 'dart:async';

class CalculatorResult{
  final List<String> history;
  final double? result;//result -> invalid

  CalculatorResult({required this.history,required this.result});
}

class Calculator{
  static final Calculator _instance=Calculator._();
  static final _operators=['+','-','*','/','%'];
  static final List<String> _history=[];
  static final StreamController<CalculatorResult> _streamController=StreamController<CalculatorResult>();


  Calculator._();
  
  factory Calculator(){
    return _instance;
  }

  get stream{
    return _streamController.stream;
  }

  void parse({required String query,bool apply=false}){
    // %, +, -, *, /, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0,.
    try{
      final result= _evaluatePostfix(_infixToPostfix(_queryToInfix(query)));
      if(apply) _history.add("$query = $result");
       _streamController.sink.add(CalculatorResult(history: _history, result:result));
    }catch(e){
      _streamController.sink.add(CalculatorResult(history: _history, result:null));
    }
  }


  List<String> _queryToInfix(String query){
    //'2+3.3*5' -> ['2','+','3.5']
    if(query.isEmpty) return [];
    
    final List<String> infix=[];
    
    String num="";//2.56+23
    for(int i=0;i<query.length;i++){//+2+3 => 5 , 3--5 => 3+(-5) => -2, wrong-> 2---3 => ["2","-","--3"] (--3 is not a number)
      if(query[i]=='.' || _isDigit(query[i]) || (num.isEmpty && _isOperator(query[i]))){
        num+=query[i];
      }else if(_isOperator(query[i])){
        if(double.tryParse(num)==null) throw Exception("invalid query");
        infix.add(num);
        num="";
        infix.add(query[i]);
      }
    }
    infix.add(num);
    return infix;
  }
  
  List<String> _infixToPostfix(List<String> infix){
    //[2,+,3,*,10,*,2,+,3]
    
    // postfix=[2,3,10,*,2,*,+,3,+]
    // operatorsStack=[]
    
    List<String> postfix=[];
    List<String> operatorsStack=[];
    
    for(var token in infix){
      if(_isOperator(token)){
        while(operatorsStack.isNotEmpty && _precedence(operatorsStack.last)>=_precedence(token)){
          postfix.add(operatorsStack.removeLast());
        }
        operatorsStack.add(token);
      }else if(double.tryParse(token)!=null){
        postfix.add(token);
      }else{
        throw Exception("Invalid infix notation");
      }
    }
    while(operatorsStack.isNotEmpty){
      postfix.add(operatorsStack.removeLast());
    }
    return postfix;
  }
  
  double _evaluatePostfix(List<String> postfix){
    try{
      // postfix=[2,3,10,*,2,*,+,3,+]
      //nums=[65]
      
      final List<double> nums=[];
      for(var val in postfix){
        if(_isOperator(val)){//x,y   [2,3,x=4,y=5]
          final y=nums.removeLast();
          final x=nums.removeLast();
          nums.add(_applyOperator(operator:val,x:x,y:y));
        }else{
          nums.add(double.parse(val));
        }
      }
      if(nums.isEmpty || nums.length>1) throw Exception("Invalid postfix");
      return nums.first;
    }catch(e){
      throw Exception("Invalid postfix");
    }
  }
  
  bool _isOperator(String op){
    return _operators.contains(op);
  }
  
  bool _isDigit(String val){
    if(val.isEmpty || val.length>1 || int.tryParse(val)==null) return false;
    return true;
  }
  
  int _precedence(String operator){
    if(operator=='+' || operator=='-'){
      return 1;
    }else if(operator=='*' || operator=='/' || operator=='%'){
      return 2;
    }
    throw Exception("Invalid operator");
  }
  
  double _applyOperator({required String operator,required double x,required double y}){
    switch(operator){
      case "+":return x+y;
      case "-":return x-y;
      case "*":return x*y;
      case "/":return x/y;
      case "%":return x%y;
      default:throw Exception("Invalid Operator");
    }
  }

  void dispose(){
    _streamController.close();
  }
}