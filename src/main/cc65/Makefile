CA=ca65
LD=ld65

all: veronica.rom 
veronica.rom: veronica.o vbasic.o
	$(LD) -C veronica.config -vm -m veronica.map -o $@ $^

%.o %.lst : %.asm
	$(CA) --listing $*.lst -o $*.o $*.asm

install: veronica.rom
	cp $^ ../../../

clean:
	rm -f *.o *.rom *.map *.lst
