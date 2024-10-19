import 'package:calculator/core/calculator.dart';
import 'package:calculator/core/themeProvider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitDown,
    DeviceOrientation.portraitUp]);
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  ThemeMode currentTheme=ThemeProvider().theme;

  @override
  void initState() {
    ThemeProvider().stream.listen((theme)=>setState(()=>currentTheme=theme));
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Calculator',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.light,
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: Colors.grey.withOpacity(0.09),
        useMaterial3: true

      ),
      themeMode: currentTheme,
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final TextEditingController _controller=TextEditingController();
  final ScrollController _historyScrollController=ScrollController();
  final Calculator calculator=Calculator();

  @override
  void initState() {
    _controller.addListener(() {
      calculator.parse(query: _controller.text);
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme=Theme.of(context);
    return Scaffold(
      body:SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.max,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
                child: Stack(
                  fit: StackFit.expand,
              children: [
                StreamBuilder<CalculatorResult>(stream: calculator.stream, builder: (context, snapshot) {
                  return Column(
                    mainAxisSize: MainAxisSize.max,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Flexible(flex: 6,
                        fit: FlexFit.tight,
                        child: ListView.builder(
                          controller: _historyScrollController,
                          padding: const EdgeInsets.all(18.0),
                          itemBuilder: (context, index)=>Padding(padding: const EdgeInsets.only(bottom: 8.0),
                            child: Text(snapshot.data!.history[index],textAlign: TextAlign.right,
                              style: const TextStyle(fontSize: 18),),),
                          itemCount: snapshot.data?.history.length ?? 0),),
                      const Divider(indent: 40,endIndent: 40,thickness: 2,),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 18.0),
                        child: TextField(
                          controller: _controller,
                          readOnly: true,
                          scribbleEnabled: true,
                          style: const TextStyle(fontSize: 40),
                          showCursor: false,
                          decoration: const InputDecoration.collapsed(hintText: ""),
                          maxLines: 1,
                          expands: false,
                          textAlign: TextAlign.right,
                        ),
                      ),
                      Padding(padding: const EdgeInsets.symmetric(horizontal: 18.0),
                      child: snapshot.data?.result!=null ? Text(snapshot.data!.result.toString(),textAlign: TextAlign.right,):
                        Text(_controller.text.isNotEmpty ? "Invalid Expression":"",textAlign: TextAlign.right,),),
                      const Divider(indent: 40,endIndent: 40,thickness: 2,height: 45,)
                    ],
                  );
                }),
                Align(alignment: Alignment.topCenter,child: IconButton(onPressed: ()=>theme.brightness==Brightness.light ? ThemeProvider().darkMode():ThemeProvider().lightMode(), icon: Icon(theme.brightness==Brightness.light ? Icons.dark_mode : Icons.light_mode)),)
              ],
            )),
            Padding(
              padding: const EdgeInsets.all(12.0).copyWith(top: 0),
              child: GridView.count(mainAxisSpacing: 10,crossAxisSpacing: 10,crossAxisCount: 4,shrinkWrap: true,children: [
                RoundedSvgButton(svgPath: "assets/buttons/c.svg",onPressed: ()=>_controller.text='',),
                RoundedSvgButton(svgPath: "assets/buttons/reminder.svg",onPressed: ()=>_controller.text+='%'),
                RoundedSvgButton(svgPath: "assets/buttons/back.svg",onPressed: (){
                  _controller.text=_controller.text.substring(0,_controller.text.length-1);
                }),
                RoundedSvgButton(svgPath: "assets/buttons/divide.svg",onPressed: ()=>_controller.text+='/'),
                RoundedSvgButton(svgPath: "assets/buttons/7.svg",onPressed: ()=>_controller.text+='7'),
                RoundedSvgButton(svgPath: "assets/buttons/8.svg",onPressed: ()=>_controller.text+='8'),
                RoundedSvgButton(svgPath: "assets/buttons/9.svg",onPressed: ()=>_controller.text+='9'),
                RoundedSvgButton(svgPath: "assets/buttons/multiply.svg",onPressed: ()=>_controller.text+='*'),
                RoundedSvgButton(svgPath: "assets/buttons/4.svg",onPressed: ()=>_controller.text+='4'),
                RoundedSvgButton(svgPath: "assets/buttons/5.svg",onPressed: ()=>_controller.text+='5'),
                RoundedSvgButton(svgPath: "assets/buttons/6.svg",onPressed: ()=>_controller.text+='6'),
                RoundedSvgButton(svgPath: "assets/buttons/subtract.svg",onPressed: ()=>_controller.text+='-'),
                RoundedSvgButton(svgPath: "assets/buttons/1.svg",onPressed: ()=>_controller.text+='1'),
                RoundedSvgButton(svgPath: "assets/buttons/2.svg",onPressed: ()=>_controller.text+='2'),
                RoundedSvgButton(svgPath: "assets/buttons/3.svg",onPressed: ()=>_controller.text+='3'),
                RoundedSvgButton(svgPath: "assets/buttons/add.svg",onPressed: ()=>_controller.text+='+'),
                RoundedSvgButton(svgPath: "assets/buttons/00.svg",onPressed: ()=>_controller.text+='00'),
                RoundedSvgButton(svgPath: "assets/buttons/0.svg",onPressed: ()=>_controller.text+='0'),
                RoundedSvgButton(svgPath: "assets/buttons/dot.svg",onPressed: ()=>_controller.text+='.'),
                RoundedSvgButton(svgPath: "assets/buttons/equal.svg",onPressed: (){
                  calculator.parse(query: _controller.text,apply: true);
                  _controller.text='';
                  WidgetsBinding.instance.addPostFrameCallback((_) => _historyScrollController.animateTo(_historyScrollController.position.maxScrollExtent, duration: const Duration(milliseconds: 300), curve: Curves.easeInOut));
                }),
              ],),
            )
          ],
        ),
      ) // This trailing comma makes auto-formatting nicer for build methods.
    );
  }

  @override
  void dispose() {
    Calculator().dispose();
    ThemeProvider().dispose();
    super.dispose();
  }
}

class RoundedSvgButton extends StatelessWidget {
  final String svgPath;
  final VoidCallback? onPressed;

  const RoundedSvgButton({
    super.key,
    required this.svgPath,
    this.onPressed
  });

  @override
  Widget build(BuildContext context) {
    final ThemeData theme=Theme.of(context);

    return ElevatedButton(onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          elevation: 2,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(35)),
          alignment: Alignment.center,
          padding: EdgeInsets.zero
        ),
        child: SvgPicture.asset(svgPath,colorFilter: ColorFilter.mode(theme.brightness==Brightness.dark ? Colors.white : Colors.black,BlendMode.srcIn),));
  }
}
