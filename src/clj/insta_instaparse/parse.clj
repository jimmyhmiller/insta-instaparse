(ns insta-instaparse.parse
  (:require [instaparse.core :as insta]
            [clojure.pprint :as pprint]
            [crustimoney.parse :as parse]))


(parse/parse )

(defn parser [grammar]
  (insta/parser grammar :auto-whitespace :standard))

(defn fixed-point [f x]
  (reduce #(if (= %1 %2) (reduced %1) %2) (iterate f x)))



(defn keep-transforming [transformer x]
  (fixed-point (partial insta/transform transformer) x))




(def calc
  {:expr          [ :sum ]
   :sum           [ :product :sum-op :sum / :product ]
   :product       [ :value :product-op :product / :value ]
   :value         [ :number / \( :expr \) ]
   :sum-op        #"(\+|-)"
   :product-op    #"(\*|/)"
   :number        #"[0-9]+"})



(parse/parse hello :hello "hello test")

(def hello
  {:hello (with-spaces "hello" :name)
   :name  #"[a-z]+"})



(def grammar

"<program> = expr | comment | program*
comment = <'//'> #'\\s.+'
phrase = phrase-type phrase-name phrase-body |
         phrase-type phrase-name phrase-args phrase-body |
         phrase-type phrase-args phrase-body
phrase-type = token
phrase-name = identifier
phrase-body = open-c expr* close-c
phrase-args = args


<expr> = fn-application | literal | val | infix-application | lambda  | phrase | map | array | token
 
map = open-c pair* close-c
pair = expr expr
array = open-s expr* close-s


fn-application = identifier open-p (expr <','?>)* close-p
infix-application = expr <' '> symbol <' '> expr |
                    expr <' '> <'`'> identifier  <'`'> <' '> expr
<symbol> = #'[$-/:-?{-~!\"^_`\\[\\]]'
keyword = <':'> identifier
identifier-base = #'[A-Za-z_][A-Za-z\\-_!?0-9]*'
identifier = !special #'[A-Za-z_][A-Za-z\\-_!?0-9]*'
<token> = special | !special identifier
special = 'fn' | 'data' | 'val'
<literal> = number | keyword | string
number = #'[0-9]+'
string = <'\"'> #'[^\"]*' <'\"'>
val = <'val'> identifier <'='> expr

fn = <'fn'> identifier open-c fn-body close-c | <'fn'> identifier lambda
fn-body = lambda+
lambda = fn-args  <'=>'> expr | fn-args <'=>'> open-c expr+ close-c
<arg> = identifier
args = open-p (arg <','?>)* close-p
fn-args = open-p ((arg | literal)  <','?>)* close-p | arg | literal

<open-s> = <'['>
<close-s> = <']'>
<open-p> = <'('>
<close-p> = <')'>
<open-c> = <'{'>
<close-c> = <'}'>")

(def text
"
val name = \"Jimmy\"

data Action {
    Increment
    Decrement
}

data Maybe {
    None
    Just(a)
} 

data Person {
    Customer(id)
    Employee(id, position)
}

fn double(x y) { x * 2 }

fn get-customer-id {
    Customer(:id) => Just(id)
    Employee(_) => None
}

fn counter {
    (state, Increment) => state + 1
    (state, Decrement) => state - 1
    (state, _) => state
}

fn div {
    (numerator, 0) => throw DivideByZero
    (numerator, divisor) => numerator / divisor
}

protocol Fn {
    invoke(f, args)
}

implement Fn(Map) {
    invoke(map, arg) {
        get(arg, map)
    }
}



implement StateReducer(Action) {
    fn reduce(state, action) {
        match(action) {
            Increment => state + 1
            Decrement => state - 1
            otherwise => state
        }
    }
}


")




(defn convert [grammar text]
  (->> ((parser grammar) text)
       (keep-transforming
        {:phrase (fn [type & args] [type (apply merge args)])
         :phrase-type keyword
         :phrase-name (fn [n] {:name n})
         :phrase-body (fn [& b] {:body b})
         :phrase-args (fn [args] {:args args})
         :data (fn [{:keys [name body]}] (map vector
                                              (repeat :constructor) 
                                              (repeat name) 
                                              body))
         :constructor (fn [parent value] (if (vector? value)
                                           [:parameterized-constructor parent (rest value)]
                                           [:val value [:obj {:parent (name parent) 
                                                              :type (name value)}]]))
         :parameterized-constructor (fn [parent [type & args]] 
                                      [:val type 
                                       [:lambda (into [:fn-args] args)
                                        [:obj (apply merge {:type (name type)
                                                            :parent (name parent)}
                                                     (map vector (map name args) args))]]])
         :string (fn [s] (str "'" s "'"))
         :fn (fn [{:keys [name args body]}] [:val name [:lambda [:fn-args args] body]])
         :identifier symbol
         :args (fn [& args] args)
         :number identity 
         :infix-application (fn [left op right] (str left " " op " " right)) 
         :keyword (fn [s] (str "'" s "'"))})))



(+ 2 2)
(parse grammar text)
(count (insta/parses (parser grammar) text))




(add-watch #'parse :pprint 
           (fn [_ _ _ _]
             (print (str (char 27) "[2J"))
             (pprint/pprint (parse grammar text))))

(add-watch #'grammar :pprint 
           (fn [_ _ _ grammar]
             (print (str (char 27) "[2J"))
             (pprint/pprint (parse grammar text))))


(add-watch #'text :pprint 
           (fn [_ _ _ text]
             (print (str (char 27) "[2J"))
             (pprint/pprint (parse grammar text))))
