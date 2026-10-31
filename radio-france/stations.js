// Catalogue de secours (flux publics officiels, hors-ligne de l'API). Certaines URL peuvent évoluer.
const R='https://icecast.radiofrance.fr/', N='https://scdn.nrjaudio.fm/adwz1/fr/';
window.STATIONS=[
 {id:'franceinter',name:'France Inter',genre:'Info',url:R+'franceinter-midfi.mp3',c:'#e2001a'},
 {id:'franceinfo',name:'franceinfo',genre:'Info',url:R+'franceinfo-midfi.mp3',c:'#0b5ed7'},
 {id:'franceculture',name:'France Culture',genre:'Culture',url:R+'franceculture-midfi.mp3',c:'#7b3fa0'},
 {id:'francemusique',name:'France Musique',genre:'Classique',url:R+'francemusique-midfi.mp3',c:'#d6336c'},
 {id:'fip',name:'FIP',genre:'Éclectique',url:R+'fip-midfi.mp3',c:'#f08c00'},
 {id:'mouv',name:"Mouv'",genre:'Hip-hop',url:R+'mouv-midfi.mp3',c:'#7048e8'},
 {id:'rtl',name:'RTL',genre:'Info',url:'https://streaming.radio.rtl.fr/rtl-1-44-128',c:'#e03131'},
 {id:'rtl2',name:'RTL2',genre:'Pop',url:'https://streaming.radio.rtl2.fr/rtl2-1-44-128',c:'#f76707'},
 {id:'fun',name:'Fun Radio',genre:'Dance',url:'https://streaming.radio.funradio.fr/fun-1-44-128',c:'#ae3ec9'},
 {id:'europe1',name:'Europe 1',genre:'Info',url:'https://stream.europe1.fr/europe1.mp3',c:'#1971c2'},
 {id:'rmc',name:'RMC',genre:'Info',url:'https://audio.bfmtv.com/rmcradio_128.mp3',c:'#e8590c'},
 {id:'nrj',name:'NRJ',genre:'Pop',url:N+'30001/mp3_128.mp3',c:'#f03e3e'},
 {id:'nostalgie',name:'Nostalgie',genre:'Années 80',url:N+'30601/mp3_128.mp3',c:'#0ca678'},
 {id:'cherie',name:'Chérie FM',genre:'Pop',url:N+'30201/mp3_128.mp3',c:'#e64980'},
 {id:'skyrock',name:'Skyrock',genre:'Hip-hop',url:'https://icecast.skyrock.net/s/natio_mp3_128k',c:'#212529'},
 {id:'classique',name:'Radio Classique',genre:'Classique',url:'https://radioclassique.ice.infomaniak.ch/radioclassique-high.mp3',c:'#862e9c'},
 {id:'jazz',name:'Jazz Radio',genre:'Jazz',url:'https://jazzradio.ice.infomaniak.ch/jazzradio-high.mp3',c:'#364fc7'},
];
