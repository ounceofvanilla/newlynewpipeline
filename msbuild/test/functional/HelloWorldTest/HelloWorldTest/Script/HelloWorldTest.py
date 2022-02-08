#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Description: This file executes HelloWorld.exe and verifies the text it
writes to stdout.

@author: Renee Williams
"""
import subprocess

def hello_world_test() -> None:
    Log.Event('Test Step 1 - Execute HelloWorld')
    proc = WshShell.Exec("..\..\..\..\HelloWorld.exe")
    Log.Event('Test Step 2 - Check HelloWorld Output')
    txt = ""
    while not proc.StdOut.AtEndOfStream:
        txt = txt + proc.StdOut.Read(1)

    Log.Event('Test Step 3 - Verify HelloWorld Output')
    if(txt.strip() != "Hello World!"):
        Log.Error(f"The incorrect phrase was output to the console: {txt}")
    else:
        Log.Checkpoint(f"The correct phrase was output to the console: {txt}")

if __name__ == "__main__":
    hello_world_test()