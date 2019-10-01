CA=ca65
LD=ld65

all: veronica.rom

veronica.rom: veronica.o
	$(LD) -C veronica.config -vm -m veronica.map -o $@ $^

veronica.o: veronica.s
	$(CA) --listing veronica.lst -o $@ $^

install: veronica.rom
	cp $^ ../../../

clean:
	rm -f veronica.o veronica.rom veronica.map veronica.lst
