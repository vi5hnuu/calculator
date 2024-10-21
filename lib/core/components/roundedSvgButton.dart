import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

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