^+v::
	SetKeyDelay, 1
	Loop, read, E:\Dropbox\Workspace 2017-2018\VariSlic3\output.txt
	{
		Loop, parse, A_LoopReadLine, %A_Tab%
		{
			SendInput, %A_LoopField%
			Send, {tab}
		}
		Send, {down}{left}{left}
	}

	ExitApp 
return